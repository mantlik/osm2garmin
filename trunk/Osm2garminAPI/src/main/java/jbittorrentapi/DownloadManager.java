/* %%Ignore-License
 * Java Bittorrent API as its name indicates is a JAVA API that implements the Bittorrent Protocol
 * This project contains two packages:
 * 1. jBittorrentAPI is the "client" part, i.e. it implements all classes needed to publish
 *    files, share them and download them.
 *    This package also contains example classes on how a developer could create new applications.
 * 2. trackerBT is the "tracker" part, i.e. it implements a all classes needed to run
 *    a Bittorrent tracker that coordinates peers exchanges. *
 *
 * Copyright (C) 2007 Baptiste Dubuis, Artificial Intelligence Laboratory, EPFL
 *
 * This file is part of jbittorrentapi-v1.0.zip
 *
 * Java Bittorrent API is free software and a free user study set-up;
 * you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Java Bittorrent API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Bittorrent API; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @version 1.0
 * @author Baptiste Dubuis
 * To contact the author:
 * email: baptiste.dubuis@gmail.com
 *
 * More information about Java Bittorrent API:
 *    http://sourceforge.net/projects/bitext/
 */
package jbittorrentapi;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Object that manages all concurrent downloads. It chooses which piece to
 * request
 * to which peer.
 */
public class DownloadManager implements DTListener, PeerUpdateListener,
        ConListenerInterface {

    // Client ID
    private byte[] clientID;
    TorrentFile torrent = null;
    private int maxConnectionNumber = 100;
    private int nbOfFiles = 0;
    private long length = 0;
    private long left = 0;
    private HashMap<Integer, Piece> pieceList;
    private BitSet isComplete;
    private BitSet isRequested;
    private int nbPieces;
    private int startPiece;
    private RandomAccessFile[] output_files;
    private PeerUpdater pu = null;
    private ConnectionListener cl = null;
    private List unchokeList = new LinkedList();
    //private List<Peer> peerList = null;
    private LinkedHashMap<String, Peer> peerList = null;
    private TreeMap<String, DownloadTask> task = null;
    private LinkedHashMap<String, BitSet> peerAvailabilies = null;
    //LinkedHashMap downloaders = new LinkedHashMap<String, Integer>(4);
    LinkedHashMap unchoken = new LinkedHashMap<String, Integer>();
    private long lastTrackerContact = 0;
    private long lastUnchoking = 0;
    private short optimisticUnchoke = 3;
    private boolean initialized = false;
    private int initProgress = 0;
    long lastPieceReceived;
    static final String WEBSEED_ID = "webseed";

    /**
     * Create a new manager according to the given torrent and using the client
     * id provided
     *
     * @param torrent TorrentFile
     * @param clientID byte[]
     * @param startPiece starting piece - 0 means from the beginning
     * @param nbPieces number of pieces to download
     */
    public DownloadManager(TorrentFile torrent, final byte[] clientID, int startPiece, int nbPieces) {
        this.clientID = clientID;
        this.peerList = new LinkedHashMap<String, Peer>();
        //this.peerList = new LinkedList<Peer>();
        this.task = new TreeMap<String, DownloadTask>();
        this.peerAvailabilies = new LinkedHashMap<String, BitSet>();

        this.torrent = torrent;
        this.nbPieces = nbPieces;
        this.startPiece = startPiece;
        this.pieceList = new HashMap<Integer, Piece>();
        this.nbOfFiles = this.torrent.length.size();

        this.isComplete = new BitSet(startPiece + nbPieces);
        this.isRequested = new BitSet(startPiece + nbPieces);
        this.output_files = new RandomAccessFile[this.nbOfFiles];

        this.length = 0;
        this.left = 0;
    }

    public DownloadManager(TorrentFile torrent, final byte[] clientID) {
        this(torrent, clientID, 0, torrent.piece_hash_values_as_binary.size());
    }

    /*
     * Initialize download.
     */
    public void init() {

        this.checkTempFiles();

        /**
         * Construct all the pieces with the correct length and hash value
         */
        int file = 0;
        long fileoffset = 0;
        for (int i = 0; i < (startPiece + nbPieces); i++) {
            initProgress = 100 * i / (startPiece + nbPieces);
            TreeMap<Integer, Long> tm = new TreeMap<Integer, Long>();
            int pieceoffset = 0;
            do {
                tm.put(file, fileoffset);
                if (fileoffset + this.torrent.getPieceLength(i) - pieceoffset
                        >= (Long) (torrent.length.get(file))
                        && i != this.nbPieces - 1) {
                    pieceoffset += ((Long) (torrent.length.get(file))).longValue() - fileoffset;
                    file++;
                    fileoffset = 0;
                    if (pieceoffset == this.torrent.getPieceLength(i)) {
                        break;
                    }
                } else {
                    fileoffset += this.torrent.getPieceLength(i) - pieceoffset;
                    break;
                }
            } while (true);
            length += torrent.getPieceLength(i);
            if (i >= startPiece) {
                pieceList.put(i, new Piece(i, torrent.getPieceLength(i),
                        16384, (byte[]) torrent.piece_hash_values_as_binary.get(i), tm));
                //System.out.println("Piece " + i + " is complete: " + this.testComplete(i));
                if (this.testComplete(i)) {
                    this.setComplete(i, true);
                } else {
                    this.left += this.pieceList.get(i).getLength();
                }
            }
        }
        this.lastUnchoking = System.currentTimeMillis();
        lastPieceReceived = System.currentTimeMillis();
        initialized = true;
    }

    public boolean testComplete(int piece) {
        boolean complete = false;
        this.pieceList.get(piece).setBlock(0, this.getPieceFromFiles(piece));
        complete = this.pieceList.get(piece).verify();
        this.pieceList.get(piece).clearData();
        return complete;
    }

    /**
     * Periodically call the unchokePeers method. This is an infinite loop.
     * User have to exit with Ctrl+C, which is not good... Todo is change this
     * method...
     */
    public void blockUntilCompletion() {
        byte[] b = new byte[0];

        while (true) {
            try {
                synchronized (b) {
                    b.wait(10000);
                    this.unchokePeers();
                    b.notifyAll();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (this.isComplete()) {
                System.out.println("\r\nSharing... Press Ctrl+C to stop client");
            }
        }
        /*
         * new IOManager().readUserInput(
         * "\r\n*****************************************\r\n" +
         * "* Press ENTER to stop sharing the files *\r\n" +
         * "*****************************************");
         */
    }

    /*
     * @return percentage of torrent init progress
     * -1 if init finished
     */
    public int init_progress() {
        if (initialized) {
            return -1;
        }
        return initProgress;
    }

    /**
     * Create and start the peer updater to retrieve new peers sharing the file
     */
    public void startTrackerUpdate() {
        if (!initialized) {
            init();
        }
        if (torrent.name.size() == 1) {
            // start webseed for 1-file torrent only; multifile webseed not implemented.
            DownloadTask dt = new WebseedTask(torrent.info_hash_as_binary, clientID, this);
            task.put(WEBSEED_ID, dt);
            dt.start();
        }
        this.pu = new PeerUpdater(this.clientID, this.torrent);
        this.pu.addPeerUpdateListener(this);
        this.pu.setListeningPort(this.cl.getConnectedPort());
        this.pu.setLeft(this.left);
        this.pu.start();
    }

    /**
     * Stop the tracker updates
     */
    public void stopTrackerUpdate() {
        this.pu.end();
    }

    /*
     * @return total length of the torrent
     */
    public long length() {
        return length;
    }

    /*
     * @return remaining data to download
     */
    public long remaining() {
        return left;
    }

    /*
     * No of bytes downloaded from PeerUpdater
     */
    public long downloaded() {
        if (pu == null) {
            return 0;
        }
        return pu.getDownloaded();
    }

    /*
     * No of bytes downloaded from PeerUpdater
     */
    public long uploaded() {
        if (pu == null) {
            return 0;
        }
        return pu.getUploaded();
    }

    /*
     * @return number of available peers
     */
    public int noOfPeers() {
        if (peerList == null) {
            return 0;
        }
        return peerList.size();
    }

    /*
     * @return number of pieces in torrent
     */
    public int noOfPieces() {
        return nbPieces;
    }

    /**
     * Create the ConnectionListener to accept incoming connection from peers
     *
     * @param minPort The minimal port number this client should listen on
     * @param maxPort The maximal port number this client should listen on
     * @return True if the listening process is started, false else
     * @todo Should it really be here? Better create it in the implementation
     */
    public boolean startListening(int minPort, int maxPort) {
        if (!initialized) {
            init();
        }
        this.cl = new ConnectionListener();
        if (this.cl.connect(minPort, maxPort)) {
            this.cl.addConListenerInterface(this);
            return true;
        } else {
            System.err.println("Could not create listening socket...");
            System.err.flush();
            return false;
        }
    }

    /**
     * Close all open files
     */
    public void closeTempFiles() {
        for (int i = 0; i < this.output_files.length; i++) {
            try {
                this.output_files[i].close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check the existence of the files specified in the torrent and if
     * necessary,
     * create them
     *
     * @return int
     * @todo Should return an integer representing some error message...
     */
    public int checkTempFiles() {
        String saveas = Constants.SAVEPATH; // Should be configurable
        if (this.nbOfFiles > 1) {
            saveas += this.torrent.saveAs + "/";
        }
        new File(saveas).mkdirs();
        for (int i = 0; i < this.nbOfFiles; i++) {
            File temp = new File(saveas + ((String) (this.torrent.name.get(i))));
            try {
                this.output_files[i] = new RandomAccessFile(temp, "rw");
                this.output_files[i].setLength((Long) this.torrent.length.get(
                        i));
                //System.out.println(temp.getPath() + " created.");
            } catch (IOException ioe) {
                System.err.println("Could not create temp files");
                ioe.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Save a piece in the corresponding file(s)
     *
     * @param piece int
     */
    public void savePiece(int piece) {
        //int remaining = this.pieceList[piece].getLength();
        byte[] data = this.pieceList.get(piece).data();
        int remainingData = data.length;
        for (Iterator it = this.pieceList.get(piece).getFileAndOffset().keySet().
                iterator(); it.hasNext();) {
            try {
                Integer file = (Integer) (it.next());
                long remaining = ((Long) this.torrent.length.get(file.intValue())).longValue()
                        - ((Long) (this.pieceList.get(piece).getFileAndOffset().
                        get(file))).longValue();
                this.output_files[file.intValue()].seek(((Long) (this.pieceList.get(piece).getFileAndOffset().get(file))).longValue());
                this.output_files[file.intValue()].write(data,
                        data.length - remainingData,
                        (remaining < remainingData) ? (int) remaining : remainingData);
                remainingData -= remaining;
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            }
        }
        data = null;
        this.pieceList.get(piece).clearData();
    }

    /**
     * Check if the current download is complete
     *
     * @return boolean
     */
    public synchronized boolean isComplete() {
        return (this.isComplete.cardinality() == this.nbPieces);
    }

    /**
     * Returns the number of pieces currently requested to peers
     *
     * @return int
     */
    public synchronized int cardinalityR() {
        return this.isRequested.cardinality();
    }

    /**
     * Returns the piece with the given index
     *
     * @param index The piece index
     * @return Piece The piece with the given index
     */
    public synchronized Piece getPiece(int index) {
        return this.pieceList.get(index);
    }

    /**
     * Check if the piece with the given index is complete and verified
     *
     * @param piece The piece index
     * @return boolean
     */
    public synchronized boolean isPieceComplete(int piece) {
        return this.isComplete.get(piece);
    }

    /**
     * Check if the piece with the given index is requested by a peer
     *
     * @param piece The piece index
     * @return boolean
     */
    public synchronized boolean isPieceRequested(int piece) {
        return this.isRequested.get(piece);
    }

    /**
     * Mark a piece as complete or not according to the parameters
     *
     * @param piece The index of the piece to be updated
     * @param is True if the piece is now complete, false otherwise
     */
    public synchronized void setComplete(int piece, boolean is) {
        this.isComplete.set(piece, is);
    }

    /**
     * Mark a piece as requested or not according to the parameters
     *
     * @param piece The index of the piece to be updated
     * @param is True if the piece is now requested, false otherwise
     */
    public synchronized void setRequested(int piece, boolean is) {
        this.isRequested.set(piece, is);
    }

    /**
     * Returns a String representing the piece being requested by peers.
     * Used only for pretty-printing.
     *
     * @return String
     */
    public synchronized String requestedBits() {
        String s = "";
        for (int i = startPiece; i < this.nbPieces; i++) {
            s += this.isRequested.get(i) ? 1 : 0;
        }
        return s;
    }

    /**
     * Returns the index of the piece that could be downloaded by the peer in
     * parameter
     *
     * @param id The id of the peer that wants to download
     * @return int The index of the piece to request
     */
    private synchronized int choosePiece2Download(String id) {
        int index = 0;
        ArrayList<Integer> possible = new ArrayList<Integer>(this.nbPieces);
        for (int i = 0; i < this.nbPieces; i++) {
            if ((!this.isPieceRequested(startPiece + i)
                    || (this.isComplete.cardinality() > this.nbPieces - 3))
                    && //(this.isRequested.cardinality() == this.nbPieces)) &&
                    (!this.isPieceComplete(startPiece + i))
                    && (WEBSEED_ID.equals(id) || this.peerAvailabilies.get(id) != null)) {

                if (WEBSEED_ID.equals(id) || this.peerAvailabilies.get(id).get(startPiece + i)) {
                    possible.add(startPiece + i);
                }
            }
        }
        //System.out.println(this.isRequested.cardinality()+" "+this.isComplete.cardinality()+" " + possible.size());
        if (possible.size() > 0) {
            Random r = new Random(System.currentTimeMillis());
            index = possible.get(r.nextInt(possible.size()));
            this.setRequested(index, true);
            return (index);
        }
        return -1;
    }

    /**
     * Removes a task and peer after the task sends a completion message.
     * Completion can be caused by an error (bad request, ...) or simply by the
     * end of the connection
     *
     * @param id Task idendity
     * @param reason Reason of the completion
     */
    public synchronized void taskCompleted(String id, int reason) {
        switch (reason) {
            case DownloadTask.CONNECTION_REFUSED:

                //System.err.println("Connection refused by host " + id);
                break;
            case DownloadTask.MALFORMED_MESSAGE:

                //System.err.println("Malformed message from " + id + ". Task ended...");
                break;
            case DownloadTask.UNKNOWN_HOST:
            //System.err.println("Connection could not be established to " + id + ". Host unknown...");

        }
        this.peerAvailabilies.remove(id);
        DownloadTask t = this.task.remove(id);
        if (t != null) {
            t.removeDTListener(this);
        }
        this.peerList.remove(id);
        //System.out.println(id+" completed reason "+DownloadTask.REASON[reason]);
        //System.err.flush();
    }

    /**
     * Received when a piece has been fully downloaded by a task. The piece
     * might
     * have been corrupted, in which case the manager will request it again
     * later.
     * If it has been successfully downloaded and verified, the piece status is
     * set to 'complete', a 'HAVE' message is sent to all connected peers and
     * the
     * piece is saved into the corresponding file(s)
     *
     * @param peerID String
     * @param i int
     * @param complete boolean
     */
    @Override
    public void pieceCompleted(String peerID, int i,
            boolean complete) {
        synchronized (this.isRequested) {
            this.isRequested.clear(i);
        }
        if (complete && !this.isPieceComplete(i)) {
            pu.updateParameters(this.torrent.getPieceLength(i), 0, "");
            synchronized (this.isComplete) {
                this.isComplete.set(i, complete);
            }
            float totaldl = (float) (((float) (100.0))
                    * ((float) (this.isComplete.cardinality()))
                    / ((float) (this.nbPieces)));

            for (Iterator it = this.task.keySet().iterator();
                    it.hasNext();) {
                try {
                    this.task.get(it.next()).ms.addMessageToQueue(
                            new Message_PP(PeerProtocol.HAVE,
                            Utils.intToByteArray(i), 1));
                } catch (NullPointerException npe) {
                }
            }
            System.out.println("Piece completed by " + peerID
                    + " : " + i + " (Total dl = " + totaldl
                    + " %)");
            this.savePiece(i);
            this.getPieceBlock(i, 0, 15000);
            if (!peerID.equals(WEBSEED_ID)) {
                this.lastPieceReceived = System.currentTimeMillis();
            }

        } else {
            System.out.println("Piece download from " + peerID + " failed: " + i);
            //this.pieceList[i].data = new byte[0];
        }

        synchronized (this) {
            if (this.isComplete.cardinality() == this.nbPieces) {
                //System.out.println("Download completed, saving file...");
                //this.save();
                //this.task.clear();
                //this.end();
                System.out.println("Task completed");
                this.notify();
            }
        }
    }

    /**
     * Set the status of the piece to requested or not
     *
     * @param i int
     * @param requested boolean
     */
    @Override
    public synchronized void pieceRequested(int i, boolean requested) {
        this.isRequested.set(i, requested);
    }

    /**
     * Choose which of the connected peers should be unchoked and authorized to
     * upload from this client. A peer gets unchoked if it is not interested, or
     * if it is interested and has one of the 5 highest download rate among the
     * interested peers. \r\n Every 3 times this method is called, calls the
     * optimisticUnchoke method, which unchoke a peer no matter its download
     * rate,
     * in a try to find a better source
     */
    public void unchokePeers() {
        int nbNotInterested = 0;
        int nbDownloaders = 0;
        int nbChoked = 0;
        this.unchoken.clear();
        List<Peer> l = new LinkedList<Peer>(this.peerList.values());
        if (!this.isComplete()) {
            Collections.sort(l, new DLRateComparator());
        } else {
            Collections.sort(l, new ULRateComparator());
        }

        for (Iterator it = l.iterator(); it.hasNext();) {
            Peer p = (Peer) it.next();
            if (p.getDLRate(false) > 0) {
                System.out.println(p + " rate: "
                        + new DecimalFormat("0.0").format(p.getDLRate(true) / (1024))
                        + " kb/s");
            }

            DownloadTask dt = this.task.get(p.toString());
            if (nbDownloaders < 5 && dt != null) {
                if (!p.isInterested()) {
                    this.unchoken.put(p.toString(), p);
                    if (p.isChoked()) {
                        dt.ms.addMessageToQueue(
                                new Message_PP(PeerProtocol.UNCHOKE));
                    }
                    p.setChoked(false);

                    while (this.unchokeList.remove(p))
                            ;
                    nbNotInterested++;
                } else if (p.isChoked()) {
                    this.unchoken.put(p.toString(), p);
                    dt.ms.addMessageToQueue(
                            new Message_PP(PeerProtocol.UNCHOKE));
                    p.setChoked(false);
                    while (this.unchokeList.remove(p))
                            ;
                    nbDownloaders++;
                }

            } else {
                if (!p.isChoked()) {
                    dt.ms.addMessageToQueue(
                            new Message_PP(PeerProtocol.CHOKE));
                    p.setChoked(true);
                }
                if (!this.unchokeList.contains(p)) {
                    this.unchokeList.add(p);
                }
                nbChoked++;
            }
            p = null;
            dt = null;
        }
        this.lastUnchoking = System.currentTimeMillis();
        if (this.optimisticUnchoke-- <= 0) {
            this.optimisticUnchoke();
            this.optimisticUnchoke = 3;
        }
    }

    private void optimisticUnchoke() {
        if (!this.unchokeList.isEmpty()) {
            Peer p = null;
            do {
                p = (Peer) this.unchokeList.remove(0);
                synchronized (this.task) {
                    DownloadTask dt = this.task.get(p.toString());
                    if (dt != null) {
                        dt.ms.addMessageToQueue(new Message_PP(PeerProtocol.UNCHOKE));
                        p.setChoked(false);
                        this.unchoken.put(p.toString(), p);
                        System.out.println(p + " optimistically unchoken...");
                    } else {
                        p = null;
                    }
                    dt = null;
                }
            } while ((p == null) && (!this.unchokeList.isEmpty()));
            p = null;
        }
    }

    /**
     * Received when a task is ready to download or upload. In such a case, if
     * there is a piece that can be downloaded from the corresponding peer, then
     * request the piece
     *
     * @param peerID String
     */
    @Override
    public void peerReady(String peerID) {
        if (System.currentTimeMillis() - this.lastUnchoking > 15000) {
            this.unchokePeers();
        }

        int piece2request = this.choosePiece2Download(peerID);
        if (piece2request != -1) {
            this.task.get(peerID).requestPiece(this.pieceList.get(piece2request));
        }
    }

    /**
     * Received when a peer request a piece. If the piece is available (which
     * should always be the case according to Bittorrent protocol) and we are
     * able and willing to upload, the send the piece to the peer
     *
     * @param peerID String
     * @param piece int
     * @param begin int
     * @param length int
     */
    @Override
    public void peerRequest(String peerID, int piece, int begin,
            int length) {
        if (this.isPieceComplete(piece)) {
            DownloadTask dt = this.task.get(peerID);
            if (dt != null) {
                dt.ms.addMessageToQueue(new Message_PP(
                        PeerProtocol.PIECE,
                        Utils.concat(Utils.intToByteArray(piece),
                        Utils.concat(Utils.intToByteArray(begin),
                        this.getPieceBlock(piece,
                        begin,
                        length)))));
                dt.peer.setULRate(length);
            }
            dt = null;
            this.pu.updateParameters(0, length, "");
        } else {
            try {
                this.task.get(peerID).end();
            } catch (Exception e) {
            }
            DownloadTask t = this.task.remove(peerID);
            if (t != null) {
                t.removeDTListener(this);
            }
            this.peerList.remove(peerID);
            this.unchoken.remove(peerID);
        }

    }

    /**
     * Load piece data from the existing files
     *
     * @param piece int
     * @return byte[]
     */
    public byte[] getPieceFromFiles(int piece) {
        byte[] data = new byte[this.pieceList.get(piece).getLength()];
        int remainingData = data.length;
        for (Iterator it = this.pieceList.get(piece).getFileAndOffset().keySet().
                iterator(); it.hasNext();) {
            try {
                Integer file = (Integer) (it.next());
                long remaining = ((Long) this.torrent.length.get(file.intValue())).longValue()
                        - ((Long) (this.pieceList.get(piece).getFileAndOffset().
                        get(file))).longValue();
                this.output_files[file.intValue()].seek(((Long) (this.pieceList.get(piece).getFileAndOffset().get(file))).longValue());
                this.output_files[file.intValue()].read(data,
                        data.length - remainingData,
                        (remaining < remainingData) ? (int) remaining : remainingData);
                remainingData -= remaining;
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            }
        }
        return data;
    }

    /**
     * Get a piece block from the existing file(s)
     *
     * @param piece int
     * @param begin int
     * @param length int
     * @return byte[]
     */
    public byte[] getPieceBlock(int piece, int begin, int length) {
        return Utils.subArray(this.getPieceFromFiles(piece), begin, length);
    }

    /**
     * Update the piece availabilities for a given peer
     *
     * @param peerID String
     * @param has BitSet
     */
    @Override
    public void peerAvailability(String peerID, BitSet has) {
        this.peerAvailabilies.put(peerID, has);
        BitSet interest = (BitSet) (has.clone());
        interest.andNot(this.isComplete);
        DownloadTask dt = this.task.get(peerID);
        if (dt != null) {
            if (interest.cardinality() > 0
                    && !dt.peer.isInteresting()) {
                dt.ms.addMessageToQueue(new Message_PP(
                        PeerProtocol.INTERESTED, 2));
                dt.peer.setInteresting(true);
            }
        }
        dt = null;
    }

    public void connect(Peer p) {
        DownloadTask dt = new DownloadTask(p,
                this.torrent.info_hash_as_binary,
                this.clientID, true,
                this.getBitField());
        dt.addDTListener(this);
        dt.start();
    }

    public void disconnect(Peer p) {
        DownloadTask dt = task.remove(p.toString());
        if (dt != null) {
            dt.end();
            dt.removeDTListener(this);
            dt = null;
        }
    }

    /**
     * Given the list in parameter, check if the peers are already present in
     * the peer list. If not, then add them and create a new task for them
     *
     * @param list LinkedHashMap
     */
    @Override
    public void updatePeerList(LinkedHashMap list) {
        //this.lastUnchoking = System.currentTimeMillis();
        synchronized (this.task) {
            //this.peerList.putAll(list);
            Set keyset = list.keySet();
            for (Iterator i = keyset.iterator(); i.hasNext();) {
                String key = (String) i.next();
                if (!this.task.containsKey(key)) {
                    Peer p = (Peer) list.get(key);
                    this.peerList.put(p.toString(), p);
                    this.connect(p);
                }
            }
        }
        System.out.println("Peer List updated from tracker with " + list.size()
                + " peers");
    }

    /**
     * Called when an update try fail. At the moment, simply display a message
     *
     * @param error int
     * @param message String
     */
    @Override
    public void updateFailed(int error, String message) {
        System.err.println(message);
        System.err.flush();
    }

    /**
     * Add the download task to the list of active (i.e. Handshake is ok) tasks
     *
     * @param id String
     * @param dt DownloadTask
     */
    @Override
    public synchronized void addActiveTask(String id, DownloadTask dt) {
        this.task.put(id, dt);
    }

    /**
     * Called when a new peer connects to the client. Check if it is already
     * registered in the peer list, and if not, create a new DownloadTask for it
     *
     * @param s Socket
     */
    @Override
    public synchronized void connectionAccepted(Socket s) {
        String id = s.getInetAddress().getHostAddress()
                + ":" + s.getPort();
        if (!this.task.containsKey(id)) {
            DownloadTask dt = new DownloadTask(null,
                    this.torrent.info_hash_as_binary,
                    this.clientID, false, this.getBitField(), s);
            dt.addDTListener(this);
            this.peerList.put(dt.getPeer().toString(), dt.getPeer());
            this.task.put(dt.getPeer().toString(), dt);
            dt.start();
        }
    }

    /**
     * Compute the bitfield byte array from the isComplete BitSet
     *
     * @return byte[]
     */
    public byte[] getBitField() {
        int l = (int) Math.ceil((double) (startPiece + nbPieces) / 8.0);
        byte[] bitfield = new byte[l];
        for (int i = 0; i < (startPiece + nbPieces); i++) {
            if (this.isComplete.get(i)) {
                bitfield[i / 8] |= 1 << (7 - i % 8);
            }
        }
        return bitfield;
    }

    public float getCompleted() {
        try {
            return (float) (((float) (100.0)) * ((float) (this.isComplete.cardinality()))
                    / ((float) (startPiece + nbPieces)));
        } catch (Exception e) {
            return 0.00f;
        }
    }

    public float getDLRate() {
        try {
            float rate = 0.00f;
            List<Peer> l = new LinkedList<Peer>(this.peerList.values());

            for (Iterator it = l.iterator(); it.hasNext();) {
                Peer p = (Peer) it.next();
                float r = p.getDLRate(false);
                if (r > 0) {
                    rate = rate + r;
                }

            }
            return rate / (1024);
        } catch (Exception e) {
            return 0.00f;
        }
    }

    public float getULRate() {
        try {
            float rate = 0.00f;
            List<Peer> l = new LinkedList<Peer>(this.peerList.values());

            for (Iterator it = l.iterator(); it.hasNext();) {
                Peer p = (Peer) it.next();
                float r = p.getULRate(false);
                if (r > 0) {
                    rate = rate + r;
                }

            }
            return rate / (1024);
        } catch (Exception e) {
            return 0.00f;
        }
    }
}
