package tern.eclipse.ide.internal.core;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;

import tern.ITernProject;
import tern.eclipse.ide.internal.core.resources.IDEResourcesManager;

public class TernAdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (IProject.class.isAssignableFrom(adaptableObject.getClass()) && adapterType == ITernProject.class) {
			try {
				return (T) IDEResourcesManager.getInstance().getTernProject(adaptableObject, false);
			} catch (IOException e) {
				return null;
			}
		}
		return null;

	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { ITernProject.class };
	}

}
