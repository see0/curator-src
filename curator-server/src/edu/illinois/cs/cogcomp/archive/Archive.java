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
public interface Archive {

	public <T extends TBase> T get(String text, boolean ws, Class<T> clazz) throws TException;
	
	public <T extends TBase> T get(List<String> text, Class<T> clazz) throws TException;
	
	public <T extends TBase> boolean store(T datum, Class<T> clazz) throws TException;
	
	public <T extends TBase> T getById(String identifier, Class<T> clazz) throws TException;
	
	public boolean close() throws TException;
}
