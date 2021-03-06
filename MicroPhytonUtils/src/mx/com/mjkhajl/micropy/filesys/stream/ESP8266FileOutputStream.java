package mx.com.mjkhajl.micropy.filesys.stream;

import java.io.IOException;
import java.io.OutputStream;

import mx.com.mjkhajl.micropy.comms.ReplHelper;
import mx.com.mjkhajl.micropy.filesys.vo.FileItem;
import mx.com.mjkhajl.micropy.utils.CodeUtils;
import mx.com.mjkhajl.micropy.utils.FileItemUtils;
import mx.com.mjkhajl.micropy.utils.Log;
import mx.com.mjkhajl.micropy.utils.Log.LogLevel;

public class ESP8266FileOutputStream extends OutputStream {

	private int			index;
	private ReplHelper	repl;
	private byte[]		buffer;

	public ESP8266FileOutputStream( FileItem file, ReplHelper repl, int buffSize ) throws IOException {

		this.index = 0;
		this.repl = repl;
		this.buffer = new byte[buffSize];

		// open the dest file in 8266
		String path = FileItemUtils.getFullPath( file );
		Log.log( "open[W]:" + path, LogLevel.INFO );
		repl.sendCommand( "file = open('" + path + "', 'wb' )" );
	}

	@Override
	public void write( int b ) throws IOException {

		buffer[index++] = (byte) b;

		if ( index >= buffer.length ) {

			flushBuffer();
		}
	}

	@Override
	public void flush() throws IOException {

		super.flush();
		flushBuffer();
	}

	private void flushBuffer() throws IOException {

		repl.sendCommand( "file.write( bytes(" + CodeUtils.byteArrayToString( buffer, 0, index ) + ") )" );
		index = 0;
	}

	@Override
	public void close() throws IOException {
		// free objects and collect garbage...
		repl.sendCommandIgnoreErrors( "file.close()" );
		repl.sendCommandIgnoreErrors( "del file" );
		repl.sendCommandIgnoreErrors( "gc.collect()" );

		super.close();
	}

}
