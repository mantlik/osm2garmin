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
package jbittorrentapi;

import java.io.*;
import java.util.Map;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Frantisek Mantlik <frantisek at mantlik.cz>
 */
public class BDecoderEncoderTest {
    
    public static byte[] torrentByteArray;
    public static byte[] torrent2ByteArray;
    
    public BDecoderEncoderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        InputStream torrentStream = BDecoderEncoderTest.class.getResourceAsStream("planet.torrent");
        File resfile = new File(BDecoderEncoderTest.class.getResource("planet.torrent").getPath());
        torrentByteArray = new byte[(int)resfile.length()];
        torrentStream.read(torrentByteArray);
        torrentStream.close();
        torrentStream = BDecoderEncoderTest.class.getResourceAsStream("planet2.torrent");
        File resfile2 = new File(BDecoderEncoderTest.class.getResource("planet2.torrent").getPath());
        torrent2ByteArray = new byte[(int)resfile2.length()];
        torrentStream.read(torrent2ByteArray);
        torrentStream.close();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of decode method, of class BDecoder.
     */
    @Test
    public void testDecode_encode_byteArr() throws Exception {
        System.out.println("decode");
        byte[] data = torrentByteArray;
        byte[] expResult = torrentByteArray;
        Map decoded = BDecoder.decode(data);
        byte[] result = BEncoder.encode(decoded);
        assertEquals("Decoded and encoded data not equal.", new String(expResult), new String(result));
    }

    /**
     * Test of decode method, of class BDecoder.
     */
    @Test
    public void testDecode_BufferedInputStream() throws Exception {
        System.out.println("decode");
        BufferedInputStream is = new BufferedInputStream(BDecoderEncoderTest.class.getResourceAsStream("planet.torrent"));
        Map expResult = BDecoder.decode(torrentByteArray);
        Map result = BDecoder.decode(is);
        assertEquals(true, BEncoder.mapsAreIdentical(expResult, result));
    }

    /**
     * Test of decodeByteArray method, of class BDecoder.
     */
    @Test
    public void testDecodeByteArray() throws Exception {
        System.out.println("decodeByteArray");
        byte[] data = torrentByteArray;
        BDecoder instance = new BDecoder();
        Map expResult = BDecoder.decode(torrentByteArray);
        Map result = instance.decodeByteArray(data);
        assertEquals(true, BEncoder.mapsAreIdentical(expResult, result));
    }

    /**
     * Test of decodeStream method, of class BDecoder.
     */
    @Test
    public void testDecodeStream() throws Exception {
        System.out.println("decodeStream");
        BufferedInputStream data = new BufferedInputStream(BDecoderEncoderTest.class.getResourceAsStream("planet.torrent"));
        BDecoder instance = new BDecoder();
        Map expResult = BDecoder.decode(torrentByteArray);
        Map result = instance.decodeStream(data);
        assertEquals(true, BEncoder.mapsAreIdentical(expResult, result));
    }

    /**
     * Test of encode method, of class BEncoder.
     */
    @Test
    public void testEncode() throws Exception {
        System.out.println("encode");
        Map object = BDecoder.decode(torrentByteArray);
        byte[] expResult = torrentByteArray;
        byte[] result = BEncoder.encode(object);
        assertEquals(new String(expResult), new String(result));
    }

    /**
     * Test of mapsAreIdentical method, of class BEncoder.
     */
    @Test
    public void testMapsAreIdentical() throws IOException {
        System.out.println("mapsAreIdentical");
        Map map1 = null;
        Map map2 = BDecoder.decode(torrentByteArray);
        boolean expResult = false;
        boolean result = BEncoder.mapsAreIdentical(map1, map2);
        assertEquals(expResult, result);
        map1 = BDecoder.decode(torrent2ByteArray);
        result = BEncoder.mapsAreIdentical(map1, map2);
        assertEquals(expResult, result);
        map1 = BDecoder.decode(torrentByteArray);
        result = BEncoder.mapsAreIdentical(map1, map2);
        expResult = true;
        assertEquals(expResult, result);
    }
}
