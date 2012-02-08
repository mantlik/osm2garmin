/*
 * #%L
 * Osm2garminAPI
 * %%
 * Copyright (C) 2011 Frantisek Mantlik <frantisek at mantlik.cz>
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
package jbittorrentapi;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;

/**
 *
 * @author fm
 */
public class Downloader extends Observable implements Runnable {
    // Max size of download buffer.

    private static final int MAX_BUFFER_SIZE = 1024;
    // These are the status names.
    /**
     *
     */
    public static final String STATUSES[] = {"Downloading",
        "Paused", "Complete", "Cancelled", "Error"};
    // These are the status codes.
    /**
     *
     */
    public static final int DOWNLOADING = 0;
    /**
     *
     */
    public static final int PAUSED = 1;
    /**
     *
     */
    public static final int COMPLETE = 2;
    /**
     *
     */
    public static final int CANCELLED = 3;
    /**
     *
     */
    public static final int ERROR = 4;
    private URL url; // download URL
    private String fileName;  // save to file of this name
    private long size; // size of download in bytes
    private long downloaded; // number of bytes downloaded
    private int status; // current status of download
    private long offset; // start of download if only part is requested

    // Constructor for Downloader.
    /**
     *
     * @param url
     * @param filename
     * @param start  
     */
    public Downloader(URL url, String filename, boolean start) {
        this.url = url;
        if (filename == null) {
            this.fileName = getFileName(url);
        } else {
            this.fileName = filename;
        }
        size = -1;
        downloaded = 0;
        File file = new File(fileName);
        if (file.exists()) {
            downloaded = file.length();
        }
        status = DOWNLOADING;

        // Begin the download.
        if (start) {
            download();
        }
    }

    // Get this download's URL.
    /**
     *
     * @return
     */
    public String getUrl() {
        return url.toString();
    }

    // Get this download's size.
    /**
     *
     * @return
     */
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
    // Get this download's progress.

    /**
     *
     * @return
     */
    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }

    // Get this download's status.
    /**
     *
     * @return
     */
    public int getStatus() {
        return status;
    }

    // Pause this download.
    /**
     *
     */
    public void pause() {
        status = PAUSED;
        stateChanged();
    }

    // Resume this download.
    /**
     *
     */
    public void resume() {
        status = DOWNLOADING;
        stateChanged();
        download();
    }

    // Cancel this download.
    /**
     *
     */
    public void cancel() {
        status = CANCELLED;
        stateChanged();
    }

    // Mark this download as having an error.
    private void error() {
        status = ERROR;
        stateChanged();
    }

    // Start or resume downloading.
    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }

    // Get file name portion of URL.
    private String getFileName(URL url) {
        String fileN = url.getFile();
        return fileN.substring(fileN.lastIndexOf('/') + 1);
    }

    // Download file.
    @Override
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;
        if (downloaded > 1) {
            downloaded--;  // overlap to process correctly fully downloaded file
        }


        try {
            // Open connection to URL.
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            // Specify what portion of file to download.
            if (size < 1) {
                connection.setRequestProperty("Range",
                        "bytes=" + (downloaded + offset) + "-");
            } else {
                connection.setRequestProperty("Range",
                        "bytes=" + (downloaded + offset) + "-" + (downloaded + offset + size - 1));
            }

            // Connect to server.
            connection.connect();

            // Make sure response code is in the 200 range.
            if (connection.getResponseCode() / 100 != 2) {
                System.out.println("Bad response code from server " + connection.getResponseCode()
                        + " - " + connection.getResponseMessage() + ".");
                error();
            }

            // Check for valid content length.
            long contentLength = Long.decode(connection.getHeaderField("content-length"));
            if (contentLength < 1) {
                System.out.println("Bad file size reported from server.");
                error();
            }

            /*
             * Set the size for this download if it
             * hasn't been already set.
             */
            if (size == -1) {
                size = contentLength + downloaded;
                stateChanged();
            }

            // Open file and seek to the end of it.
            file = new RandomAccessFile(fileName, "rw");
            file.seek(downloaded);

            stream = connection.getInputStream();
            while (status == DOWNLOADING) {
                /*
                 * Size buffer according to how much of the
                 * file is left to download.
                 */
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[(int) (size - downloaded)];
                }

                // Read from server into buffer.
                int read = stream.read(buffer);
                if (read == -1) {
                    break;
                }

                // Write buffer to file.
                file.write(buffer, 0, read);
                downloaded += read;
                stateChanged();
                if (downloaded >= size) {
                    break;
                }
            }

            /*
             * Change status to complete if this point was
             * reached because downloading has finished.
             */
            if (status == DOWNLOADING) {
                status = COMPLETE;
                stateChanged();
            }
        } catch (Exception e) {
            System.out.println("Download error.");
            error();
        } finally {
            // Close file.
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {
                }
            }

            // Close connection to server.
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    // Notify observers that this download's status has changed.
    private void stateChanged() {
        setChanged();
        notifyObservers();
    }
}
