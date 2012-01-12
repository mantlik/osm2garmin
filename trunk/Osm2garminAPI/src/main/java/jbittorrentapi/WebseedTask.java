/*
 * #%L
 * Osm2garminAPI
 * %%
 * Copyright (C) 2011 - 2012 Frantisek Mantlik <frantisek at mantlik.cz>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
/*
 * Copyright (C) 2012 Frantisek Mantlik <frantisek at mantlik.cz>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package jbittorrentapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 *
 * @author Frantisek Mantlik <frantisek at mantlik.cz>
 */
public class WebseedTask extends DownloadTask {

    private DownloadManager manager;
    private static final long MAX_IDLE_TIME = 120000;  // start download when no piece received 2 min
    public static boolean webseedActive = false;

    public WebseedTask(byte[] fileID, byte[] myID, DownloadManager manager) {
        super(null, fileID, myID, false, null);
        this.manager = manager;
    }

    @Override
    public void run() {
        while (run) {
            try {
                if ((System.currentTimeMillis() - manager.lastPieceReceived) > MAX_IDLE_TIME) {
                    webseedActive = true;
                    manager.peerReady(DownloadManager.WEBSEED_ID);
                    if (downloadPiece != null) {
                        boolean breakDownload = false;
                        downloadPiece.clearData();
                        TreeMap<Integer, Long> offsets = (TreeMap<Integer, Long>) downloadPiece.getFileAndOffset();
                        ArrayList<String> urls = manager.torrent.urlList;
                        int index = (int) (Math.random() * urls.size());
                        String url = urls.get(index);
                        int fileIndex = offsets.firstKey();
                        for (int i = 0; i < offsets.size(); i++) {
                            long offs = offsets.get(fileIndex);
                            String tempFileName = File.createTempFile("piece", ".tmp").getAbsolutePath();
                            String fileUrl = url;
                            if (manager.torrent.name.size() > 1) {  // multifile torrent
                                fileUrl += "/" + manager.torrent.name.get(fileIndex);
                            }
                            Downloader downloader = new Downloader(new URL(fileUrl), tempFileName, false);
                            downloader.setSize(downloadPiece.getLength());
                            downloader.setOffset(offs);
                            downloader.run();
                            if (downloader.getStatus() != Downloader.COMPLETE) {
                                manager.pieceCompleted(DownloadManager.WEBSEED_ID, downloadPiece.getIndex(), false);
                                breakDownload = true;
                                continue;
                            }
                            InputStream is = new FileInputStream(tempFileName);
                            byte[] block = new byte[downloadPiece.getLength()];
                            is.read(block);
                            is.close();
                            new File(tempFileName).delete();
                            downloadPiece.setBlock(i, block);
                            if (fileIndex < offsets.lastKey()) {
                                fileIndex = offsets.higherKey(fileIndex);
                            }
                        }
                        if (breakDownload) {
                            continue;
                        }
                        if (downloadPiece.verify()) {
                            manager.pieceCompleted(DownloadManager.WEBSEED_ID, downloadPiece.getIndex(), true);
                        } else {
                            manager.pieceCompleted(DownloadManager.WEBSEED_ID, downloadPiece.getIndex(), false);
                        }
                    }
                } else {
                    webseedActive = false;
                }
                Thread.sleep(1000);
            } catch (IOException ex) {
                if (downloadPiece != null) {
                    manager.pieceCompleted(DownloadManager.WEBSEED_ID, downloadPiece.getIndex(), false);
                }
            } catch (InterruptedException ex) {
            }
        }
    }
}
