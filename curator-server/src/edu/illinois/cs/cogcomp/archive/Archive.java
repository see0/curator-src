package edu.illinois.cs.cogcomp.archive;

import java.util.List;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;

/**
 * Archive interface. Curators have an archive in which they store records.
 * 
 * @author James Clarke
 * 
 */
/**
 * @author james
 *
 */
public interface Archive {


	/**
	 * Retrieve a <code>T</code> representing the text String and whether the text
	 * is whitespace tokenized.
	 * @param <T> Generic to return
	 * @param text String of text
	 * @param ws true if text is whitespace tokenized
	 * @param clazz class to return
	 * @return
	 * @throws ArchiveException
	 */
	public <T extends TBase> T get(String text, boolean ws, Class<T> clazz) throws ArchiveException;
	
	/**
	 * Retrieve a <code>T</code> representing the List of String texts
	 * @param <T> Generic to return
	 * @param text String of text
	 * @param clazz class to return
	 * @return
	 * @throws ArchiveException
	 */
	public <T extends TBase> T get(List<String> text, Class<T> clazz) throws ArchiveException;
	
	
	/**
	 * Store a <code>T</code> datum in the archive. Class is the type of <code>T</code>.
	 * @param <T>
	 * @param datum
	 * @param clazz
	 * @return
	 * @throws ArchiveException
	 */
	public <T extends TBase> boolean store(T datum, Class<T> clazz) throws ArchiveException;
	
	
	/**
	 * Retrieve a <code>T</code> represented by the identifier.
	 * @param <T>
	 * @param identifier
	 * @param clazz
	 * @return
	 * @throws ArchiveException
	 */
	public <T extends TBase> T getById(String identifier, Class<T> clazz) throws ArchiveException;
	
	/**
	 * Some archive implementations may need to be closed.
	 * @return
	 * @throws TException
	 */
	public boolean close() throws ArchiveException;
}
