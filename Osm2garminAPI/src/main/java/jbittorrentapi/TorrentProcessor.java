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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.openide.util.Exceptions;

/**
 *
 * Class enabling to process a torrent file
 *
 * @author Baptiste Dubuis
 * @version 0.1
 */
public class TorrentProcessor {

    private TorrentFile torrent;

    public TorrentProcessor(TorrentFile torrent) {
        this.torrent = torrent;
    }

    public TorrentProcessor() {
        this.torrent = new TorrentFile();
    }

    /**
     * Given the path of a torrent, parse the file and represent it as a Map
     *
     * @param filename String
     * @return Map
     */
    public Map parseTorrent(String filename) {
        return this.parseTorrent(new File(filename));
    }

    /**
     * Given a File (supposed to be a torrent), parse it and represent it as a
     * Map
     *
     * @param file File
     * @return Map
     */
    public Map parseTorrent(File file) {
        try {
            return BDecoder.decode(IOManager.readBytesFromFile(file));
        } catch (IOException ioe) {
        }
        return null;
    }

    /**
     * Given a Map, retrieve all useful information and represent it as a
     * TorrentFile object
     *
     * @param m Map
     * @return TorrentFile
     */
    public TorrentFile getTorrentFile(Map m) {
        if (m == null) {
            return null;
        }
        if (m.containsKey("announce")) // mandatory key
        {
            this.torrent.announceURL = new String((byte[]) m.get("announce"));
        } else {
            return null;
        }
        if (m.containsKey("comment")) // optional key
        {
            this.torrent.comment = new String((byte[]) m.get("comment"));
        }
        if (m.containsKey("created by")) // optional key
        {
            this.torrent.createdBy = new String((byte[]) m.get("created by"));
        }
        if (m.containsKey("creation date")) // optional key
        {
            this.torrent.creationDate = (Long) m.get("creation date");
        }
        if (m.containsKey("encoding")) // optional key
        {
            this.torrent.encoding = new String((byte[]) m.get("encoding"));
        }
        if (m.containsKey("announce-list")) { // extension
            List tiers = (List) m.get("announce-list");
            if ((tiers != null) && (!tiers.isEmpty())) {
                // simplified processing - consider first tier only
                for (int j = 0; j < tiers.size(); j++) {
                    List tier = (List) tiers.get(j);
                    if (torrent.announceList.size() < j + 1) {
                        torrent.announceList.add(new ArrayList<String>());
                    }
                    for (int i = 0; i < tier.size(); i++) {
                        String announce = new String((byte[]) tier.get(i));
                        if (!torrent.announceList.get(j).contains(announce)) {
                            torrent.announceList.get(j).add(announce);
                        }
                    }
                }
            }
        }
        torrent.changeAnnounce();

        if (m.containsKey("url-list")) { // extension
            List urls = (List) m.get("url-list");
            if ((urls != null) && (!urls.isEmpty())) {
                for (int j = 0; j < urls.size(); j++) {
                    String url = new String((byte[]) urls.get(j));
                    torrent.urlList.add(url);
                }
            }
        }

        //Store the info field data
        if (m.containsKey("info")) {
            Map info = (Map) m.get("info");
            try {

                this.torrent.info_hash_as_binary = Utils.hash(BEncoder.encode(info));
                this.torrent.info_hash_as_hex = Utils.byteArrayToByteString(
                        this.torrent.info_hash_as_binary);
                this.torrent.info_hash_as_url = Utils.byteArrayToURLString(
                        this.torrent.info_hash_as_binary);
            } catch (IOException ioe) {
                return null;
            }
            if (info.containsKey("name")) {
                this.torrent.saveAs = new String((byte[]) info.get("name"));
            }
            if (info.containsKey("piece length")) {
                this.torrent.setPieceLength(0, (int) ((Long) info.get("piece length")).longValue());
            } else {
                return null;
            }

            if (info.containsKey("pieces")) {
                byte[] piecesHash2 = (byte[]) info.get("pieces");
                if (piecesHash2.length % 20 != 0) {
                    return null;
                }

                for (int i = 0; i < piecesHash2.length / 20; i++) {
                    byte[] temp = Utils.subArray(piecesHash2, i * 20, 20);
                    this.torrent.piece_hash_values_as_binary.put(i, temp);
                    this.torrent.piece_hash_values_as_hex.put(i, Utils.byteArrayToByteString(
                            temp));
                    this.torrent.piece_hash_values_as_url.put(i, Utils.byteArrayToURLString(
                            temp));
                }
            } else {
                return null;
            }

            if (info.containsKey("files")) {
                List multFiles = (List) info.get("files");
                this.torrent.total_length = 0;
                for (int i = 0; i < multFiles.size(); i++) {
                    this.torrent.length.add(((Long) ((Map) multFiles.get(i)).get("length")).longValue());
                    this.torrent.total_length += ((Long) ((Map) multFiles.get(i)).get("length")).longValue();

                    List path = (List) ((Map) multFiles.get(i)).get(
                            "path");
                    String filePath = "";
                    for (int j = 0; j < path.size(); j++) {
                        filePath += new String((byte[]) path.get(j));
                    }
                    this.torrent.name.add(filePath);
                }
            } else {
                this.torrent.length.add(((Long) info.get("length")).longValue());
                this.torrent.total_length = ((Long) info.get("length")).longValue();
                this.torrent.name.add(new String((byte[]) info.get("name")));
            }
            // Fill in piece lengths
            torrent.setPieceLength(torrent.piece_hash_values_as_binary.size() - 1, torrent.getPieceLength(0));
            torrent.setPieceLength(torrent.piece_hash_values_as_binary.size() - 1,
                    (int) (torrent.total_length % torrent.getPieceLength(0)));
        } else {
            return null;
        }
        return this.torrent;
    }

    /**
     * Sets the TorrentFile object of the Publisher equals to the given one
     *
     * @param torr TorrentFile
     */
    public void setTorrent(TorrentFile torr) {
        this.torrent = torr;
    }

    /**
     * Updates the TorrentFile object according to the given parameters
     *
     * @param url The announce url
     * @param pLength The length of the pieces of the torrent
     * @param comment The comments for the torrent
     * @param encoding The encoding of the torrent
     * @param filename The path of the file to be added to the torrent
     */
    public void setTorrentData(String url, int pLength, String comment,
            String encoding, String filename) {
        this.torrent.announceURL = url;
        this.torrent.setPieceLength(0, pLength * 1024);
        this.torrent.createdBy = Constants.CLIENT;
        this.torrent.comment = comment;
        this.torrent.creationDate = System.currentTimeMillis();
        this.torrent.encoding = encoding;
        this.addFile(filename);
    }

    /**
     * Updates the TorrentFile object according to the given parameters
     *
     * @param url The announce url
     * @param pLength The length of the pieces of the torrent
     * @param comment The comments for the torrent
     * @param encoding The encoding of the torrent
     * @param name The name of the directory to save the files in
     * @param filenames The path of the file to be added to the torrent
     * @throws java.lang.Exception
     */
    public void setTorrentData(String url, int pLength, String comment,
            String encoding, String name, List filenames) throws Exception {
        this.torrent.announceURL = url;
        this.torrent.setPieceLength(0, pLength * 1024);
        this.torrent.comment = comment;
        this.torrent.createdBy = Constants.CLIENT;
        this.torrent.creationDate = System.currentTimeMillis();
        this.torrent.encoding = encoding;
        this.torrent.saveAs = name;
        this.addFiles(filenames);
    }

    /**
     * Sets the announce url of the torrent
     *
     * @param url String
     */
    public void setAnnounceURL(String url) {
        this.torrent.announceURL = url;
    }

    /**
     * Sets the pieceLength
     *
     * @param length int
     */
    public void setPieceLength(int piece, int length) {
        this.torrent.setPieceLength(piece, length * 1024);
    }

    /**
     * Sets the directory the files have to be saved in (in case of multiple
     * files torrent)
     *
     * @param name String
     */
    public void setName(String name) {
        this.torrent.saveAs = name;
    }

    /**
     * Sets the comment about this torrent
     *
     * @param comment String
     */
    public void setComment(String comment) {
        this.torrent.comment = comment;
    }

    /**
     * Sets the creator of the torrent. This should be the client name and
     * version
     *
     * @param creator String
     */
    public void setCreator(String creator) {
        this.torrent.createdBy = creator;
    }

    /**
     * Sets the time the torrent was created
     *
     * @param date long
     */
    public void setCreationDate(long date) {
        this.torrent.creationDate = date;
    }

    /**
     * Sets the encoding of the torrent
     *
     * @param encoding String
     */
    public void setEncoding(String encoding) {
        this.torrent.encoding = encoding;
    }

    /**
     * Add the files in the list to the torrent
     *
     * @param l A list containing the File or String object representing the
     * files to be added
     * @return int The number of files that have been added
     * @throws Exception
     */
    public int addFiles(List l) throws Exception {
        return this.addFiles(l.toArray());
    }

    /**
     * Add the files in the list to the torrent
     *
     * @param file The file to be added
     * @return int The number of file that have been added
     * @throws Exception
     */
    public int addFile(File file) {
        return this.addFiles(new File[]{file});
    }

    /**
     * Add the files in the list to the torrent
     *
     * @param filename The path of the file to be added
     * @return int The number of file that have been added
     * @throws Exception
     */
    public int addFile(String filename) {
        return this.addFiles(new String[]{filename});
    }

    /**
     * Add the files in the list to the torrent
     *
     * @param filenames An array containing the files to be added
     * @return int The number of files that have been added
     * @throws Exception
     */
    public int addFiles(Object[] filenames) {
        int nbFileAdded = 0;

        if (this.torrent.total_length == -1) {
            this.torrent.total_length = 0;
        }

        for (int i = 0; i < filenames.length; i++) {
            File f = null;
            if (filenames[i] instanceof String) {
                f = new File((String) filenames[i]);
            } else if (filenames[i] instanceof File) {
                f = (File) filenames[i];
            }
            if (f != null) {
                if (f.exists()) {
                    this.torrent.total_length += f.length();
                    this.torrent.name.add(f.getPath());
                    this.torrent.length.add(new Long(f.length()).longValue());
                    nbFileAdded++;
                }
            }
        }
        return nbFileAdded;
    }

    /**
     * Generate the SHA-1 hashes for the file in the torrent in parameter
     *
     * @param torr TorrentFile
     */
    public void generatePieceHashes(TorrentFile torr) {
        ByteBuffer bb = ByteBuffer.allocate(torr.getPieceLength(0));
        int index = 0;
        long total = 0;
        torr.piece_hash_values_as_binary.clear();
        for (int i = 0; i < torr.name.size(); i++) {
            total += (Long) torr.length.get(i);
            File f = new File((String) torr.name.get(i));
            if (f.exists()) {
                try {
                    FileInputStream fis = new FileInputStream(f);
                    int read = 0;
                    byte[] data = new byte[torr.getPieceLength(0)];
                    while ((read = fis.read(data, 0, bb.remaining())) != -1) {
                        bb.put(data, 0, read);
                        if (bb.remaining() == 0) {
                            torr.piece_hash_values_as_binary.put(index++, Utils.hash(bb.array()));
                            bb.clear();
                        }
                    }
                } catch (FileNotFoundException fnfe) {
                } catch (IOException ioe) {
                }
            }
        }
        if (bb.remaining() != bb.capacity()) {
            torr.piece_hash_values_as_binary.put(index++, Utils.hash(Utils.subArray(
                    bb.array(), 0, bb.capacity() - bb.remaining())));
        }
    }

    /**
     * Generate the SHA-1 hashes for the file in the torrent in parameter The
     * file is not saved locally
     *
     * @param torr TorrentFile
     * @param remote_urls Server addresses to be used to download the file from
     * @param md5list Optional MD5 hash, null if not provided
     * @return true if successful
     */
    public boolean generatePieceHashesFromRemote(TorrentFile torr, String[] remote_urls, String[] md5list) {
        ByteBuffer bb = ByteBuffer.allocate(torr.getPieceLength(0));
        int index = 0;
        long total = 0;
        MessageDigest md = null;
        torr.piece_hash_values_as_binary.clear();
        for (int i = 0; i < torr.name.size(); i++) {
            String md5 = null;
            if (md5list != null) {
                md5 = md5list[i];
            }
            boolean compute_md5 = md5 != null;
            long length = torr.length.get(i);
            total += length;
            String fname = (String) torr.name.get(i);
            long processed = 0;
            if (compute_md5) {
                try {
                    md = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException ex) {
                    Exceptions.printStackTrace(ex);
                    return false;
                }
            }
            while (processed < length) {
                InputStream fis = null;
                try {
                    URL url = new URL(remote_urls[(int) (Math.random() * remote_urls.length)] + "/" + fname);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("Range",
                            "bytes=" + processed + "-");
                    connection.connect();
                    if (connection.getResponseCode() / 100 != 2) {
                        System.out.println("Bad response code from server " + connection.getResponseCode()
                                + " - " + connection.getResponseMessage() + ".");
                        continue;
                    }
                    long contentLength = Long.decode(connection.getHeaderField("content-length"));
                    if (contentLength < 1) {
                        System.out.println("Bad file size reported from server.");
                        continue;
                    }
                    fis = connection.getInputStream();
                    int read = 0;
                    int piece = (int) (processed / torrent.getPieceLength(0));
                    int pieces = (int) (torr.length.get(i) / torrent.getPieceLength(0));
                    byte[] data = new byte[torr.getPieceLength(0)];
                    System.out.println("Starting data download from " + url.toExternalForm());
                    while ((read = fis.read(data, 0, bb.remaining())) != -1) {
                        if (compute_md5 && read > 0) {
                            md.update(data, 0, read);
                        }
                        bb.put(data, 0, read);
                        processed += read;
                        if (bb.remaining() == 0) {
                            torr.piece_hash_values_as_binary.put(index++, Utils.hash(bb.array()));
                            bb.clear();
                            piece++;
                            System.out.print("Piece " + piece + " / " + pieces + "          \r");
                        }
                    }
                    fis.close();
                    System.out.println();
                    System.out.println("Finished download from " + url.toExternalForm());
                    if (compute_md5) {
                        byte[] mdbytes = md.digest();
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < mdbytes.length; j++) {
                            sb.append(Integer.toString((mdbytes[j] & 0xff) + 0x100, 16).substring(1));
                        }
                        if (!md5.trim().toLowerCase().contains(sb.toString().toLowerCase())) {
                            System.out.println(md5 + " != " + sb.toString());
                            System.out.println("MD5 checksum of the file " + fname + " failed.");
                            return false;
                        } else {
                            System.out.println("MD5 checksum of the file " + fname +" OK.");
                        }
                    }
                } catch (MalformedURLException ex) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ex1) {
                            Exceptions.printStackTrace(ex1);
                        }
                    }
                    System.out.println("\r\n" + ex.getMessage() + " Retrying.");
                } catch (IOException ex) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ex1) {
                            Exceptions.printStackTrace(ex1);
                        }
                    }
                    System.out.println("\r\n" + ex.getMessage() + " Retrying.");
                }
            }
        }
        if (bb.remaining() != bb.capacity()) {
            torr.piece_hash_values_as_binary.put(index++, Utils.hash(Utils.subArray(
                    bb.array(), 0, bb.capacity() - bb.remaining())));
        }
        return true;
    }

    /**
     * Generate the SHA-1 hashes for the file in the torrent in parameter The
     * file is not saved locally
     *
     * @param remote_urls Server address to be used to download the file from
     * @param md5 Optional MD5 hash list, null if not provided
     * @return true if successful
     */
    public boolean generatePieceHashesFromRemote(String[] remote_urls, String[] md5) {
        return this.generatePieceHashesFromRemote(torrent, remote_urls, md5);
    }

    /**
     * Generate the SHA-1 hashes for the files in the current object TorrentFile
     */
    public void generatePieceHashes() {
        this.generatePieceHashes(this.torrent);
    }

    /**
     * Generate the bytes of the bencoded TorrentFile data
     *
     * @param torr TorrentFile
     * @return byte[]
     */
    public byte[] generateTorrent(TorrentFile torr) {
        SortedMap map = new TreeMap();
        map.put("announce", torr.announceURL);
        if (torr.comment.length() > 0) {
            map.put("comment", torr.comment);
        }
        if (torr.creationDate >= 0) {
            map.put("creation date", torr.creationDate);
        }
        if (torr.createdBy.length() > 0) {
            map.put("created by", torr.createdBy);
        }
        
        if (! torr.announceList.isEmpty()) {
            map.put("announce-list", torr.announceList);
        }
        
        if (! torr.urlList.isEmpty()) {
            map.put("url-list", torr.urlList);
        }

        SortedMap info = new TreeMap();
        if (torr.name.size() == 1) {
            info.put("length", (Long) torr.length.get(0));
            info.put("name", new File((String) torr.name.get(0)).getName());
        } else {
            if (!torr.saveAs.matches("")) {
                info.put("name", torr.saveAs);
            } else {
                info.put("name", "noDirSpec");
            }
            ArrayList files = new ArrayList();
            for (int i = 0; i < torr.name.size(); i++) {
                SortedMap file = new TreeMap();
                file.put("length", (Long) torr.length.get(i));
                String[] path = ((String) torr.name.get(i)).split("\\\\");
                File f = new File((String) (torr.name.get(i)));

                ArrayList pathList = new ArrayList(path.length);
                for (int j = (path.length > 1) ? 1 : 0; j < path.length; j++) {
                    pathList.add(path[j]);
                }
                file.put("path", pathList);
                files.add(file);
            }
            info.put("files", files);
        }
        info.put("piece length", torr.getPieceLength(0));
        byte[] pieces = new byte[0];
        for (int i = 0; i < torr.piece_hash_values_as_binary.size(); i++) {
            pieces = Utils.concat(pieces,
                    (byte[]) torr.piece_hash_values_as_binary.get(i));
        }
        info.put("pieces", pieces);
        map.put("info", info);
        try {
            byte[] data = BEncoder.encode(map);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Generate the bytes for the current object TorrentFile
     *
     * @return byte[]
     */
    public byte[] generateTorrent() {
        return this.generateTorrent(this.torrent);
    }

    /**
     * Returns the local TorrentFile in its current state
     *
     * @return TorrentFile
     */
    public TorrentFile getTorrent() {
        return this.torrent;
    }
}
