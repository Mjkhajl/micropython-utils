package mx.com.mjkhajl.micropy.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileSystemSynchronizerESP8266Test {

	FileSystemSynchronizerESP8266	sync;

	private static final File		TEST_DIR_ROOT	= new File( "C:/Users/Luis Miguel/git/MicroPytonUtils/MicroPhytonUtils/web-server" );

	@Before
	public void setUp() throws Throwable {

		/* @formatter:off
		 * 
		 *  TIMEOUT: 200, 
		 *  SPEED: 115200 BPS, 
		 *  DATA BITS: 8, 
		 *  STOP BITS: 1, 
		 *  PARITY: NONE (0),
		 *  LAZYTIME: 500
		 *  
		 * @formatter:on	
		 */
		sync = new FileSystemSynchronizerESP8266( 2000, 115200, 8, 1, 0, 10, 96 );
	}

	@Test
	public void synchronizeFs() throws Exception {

		sync.synchronizeFs( TEST_DIR_ROOT, "/web-server" );
	}

	@Test
	public void writeFile() throws Exception {

		sync.writeFile( new File( TEST_DIR_ROOT, "web-server.py" ), "/web-server/web-server.py" );
	}

	@Test
	public void readFile() throws Exception {

		sync.readFile( "/web-server/web-server.py" );
	}

	@Test
	public void readBinary() throws Exception {

		byte[] binary = sync.readFile( "/web-server/img.jpg" );

		FileOutputStream foStream = null;

		try {
			
			foStream = new FileOutputStream( new File( TEST_DIR_ROOT, "img-down.jpg" ) );
			foStream.write( binary );
			foStream.flush();
			
		} finally {

			CodeUtils.close( foStream );
		}
	}

	@Test
	public void binaryIntegrity() throws Exception {

		String testFile = "web-server.py";
		//String testFile = "img.jpg";
		
		String remoteFile = "/web-server/" + testFile;
		File localFile = new File( TEST_DIR_ROOT, testFile );

		sync.writeFile( localFile, remoteFile );

		byte[] readBytes = sync.readFile( remoteFile );
		byte[] localBytes = readLocalFile( localFile );

		compareBytes( readBytes, localBytes );
	}

	private void compareBytes( byte[] a, byte[] b ) {

		System.out.println( "a.length: " + a.length );
		System.out.println( "b.length: " + a.length );

		for ( int i = 0; i < b.length; i++ ) {

			if ( (int) a[i] != b[i] ) {

				System.out.println( "diference at: [" + i + "] a value: " + a[i] + "(" + (char) a[i] + ") b value: " + b[i] + "(" + (char) b[i] + ")" );
			}
		}
	}

	private byte[] readLocalFile( File file ) throws Exception {

		FileInputStream finStream = null;
		ByteArrayOutputStream baoStream = null;

		try {

			finStream = new FileInputStream( file );
			baoStream = new ByteArrayOutputStream();

			byte[] buffer = new byte[1024];
			int readBytes = -1;

			while ( ( readBytes = finStream.read( buffer ) ) != -1 ) {

				baoStream.write( buffer, 0, readBytes );
			}
			baoStream.flush();

			return baoStream.toByteArray();

		} finally {

			CodeUtils.close( finStream, baoStream );
		}
	}

	@After
	public void end() throws Throwable {

		CodeUtils.close( sync );
	}

}
