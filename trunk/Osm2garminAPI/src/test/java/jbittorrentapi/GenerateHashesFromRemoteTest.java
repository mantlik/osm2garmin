/*
 * #%L
 * Osm2garminAPI
 * %%
 * Copyright (C) 2011 - 2014 Frantisek Mantlik <frantisek at mantlik.cz>
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
 * Copyright (C) 2014 frantisek
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author frantisek
 */
public class GenerateHashesFromRemoteTest {

    private static final String[] MIRRORS = {
        "http://planet.openstreetmap.org/notes"
    };
    private static final String FILENAME = "planet-notes-latest.osn.bz2";
    private static final int PIECE_LENGTH = 1024 * 1024;

    @Ignore
    @Test
    public void testGenerateHashes() throws MalformedURLException, IOException, NoSuchAlgorithmException {
        System.out.println("Test Generate Hashes from Remote File");
        TorrentFile torrent = new TorrentFile();
        torrent.creationDate = System.currentTimeMillis() / 1000;
        torrent.urlList.addAll(Arrays.asList(MIRRORS));
        torrent.setPieceLength(0, PIECE_LENGTH);
        torrent.name.add(FILENAME);
        URL url = new URL(MIRRORS[0] + "/" + FILENAME);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        long length = connection.getContentLengthLong();
        torrent.length.add(length);
        connection = (HttpURLConnection) new URL(MIRRORS[0] + "/" + FILENAME + ".md5").openConnection();
        Scanner scanner = new Scanner(connection.getInputStream());
        String md5 = scanner.nextLine().substring(0, 32);
        scanner.close();
        System.out.println("MD5:" + md5);
        TorrentProcessor tp = new TorrentProcessor(torrent);
        ArrayList<String> md5list = new ArrayList<String>();
        md5list.add(md5);
        assertTrue("Hashes generation failed.", tp.generatePieceHashesFromRemote(MIRRORS,
                md5list.toArray(new String[0])));
    }
}
