package mx.com.mjkhajl.micropy.comms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import mx.com.mjkhajl.micropy.filesys.FileSystemSynchronizer;
import mx.com.mjkhajl.micropy.filesys.vo.FileItem;
import mx.com.mjkhajl.micropy.utils.Log;
import mx.com.mjkhajl.micropy.utils.Log.LogLevel;

public class ReplJavaCommandConsole {

	private final ReplHelper				repl;
	private final FileSystemSynchronizer	sync;

	public ReplJavaCommandConsole( ReplHelper repl, FileSystemSynchronizer sync ) {

		this.repl = repl;
		this.sync = sync;
	}

	public void start( FileItem src, FileItem dest ) throws IOException {

		BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );

		String line;

		while ( !"exit".equals( line = reader.readLine() ) ) {

			try {
				switch ( line ) {

					case "java sync":
						sync.synchronizeDir( src, dest );
						continue;
					case "log level":
						System.out.println( "set level?" );
						Log.setLogLevelFromArgs( new String[] { reader.readLine() } );
						continue;
					default:
						System.out.println( repl.sendCommand( line ) );
				}
			} catch ( Exception e ) {

				Log.log( e, LogLevel.ERROR );
			}
			System.out.print( ">>>" );
		}

		Log.log( "console closed...", LogLevel.INFO );
	}
}