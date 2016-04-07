/**
 *  Copyright (c) 2013-2016 Angelo ZERR and Genuitec LLC.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Piotr Tomiak <piotr@genuitec.com> - initial API and implementation
 */
package tern.eclipse.ide.internal.core.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;

import tern.ITernFile;
import tern.ITernProject;
import tern.TernResourcesManager;
import tern.resources.AbstractTernFile;
import tern.utils.IOUtils;

public class IDETernFile extends AbstractTernFile implements ITernFile {
	
	private IFile iFile;
	private IDocument document;
	private boolean docLoaded;
	
	public IDETernFile(IFile file) {
		this.iFile = file;
	}
	
	@Override
	public String getFullName(ITernProject project) {
		IPath location = iFile.getLocation();
		if (location==null) {
			IPath leadingPaths = new Path("");
			IProject prj = null;
			IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(iFile.getLocationURI());
			IContainer linkContainer = containers[0];
			// look for the tern project file marker in resource paths, track leading paths
			IPath parent = linkContainer.getProject().getLocation();
			while (parent.segmentCount() > 0) {
				String last = parent.lastSegment();
				parent = parent.removeLastSegments(1);
				if (parent.append(".tern-project").toFile().exists()) {
					IContainer container = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(parent);
					if (container != null) {
						if (container instanceof IProject) {
							prj = (IProject)container;
							break;
						}
					}
				} else {
					leadingPaths.append(last);
				}
			}
			if (prj!=null) {
				String name = leadingPaths.append(iFile.getFullPath()).makeRelative().toString();
				return name;
			}
		
			return PROJECT_PROTOCOL + iFile.getFullPath().makeRelative().toString();
		} else {
			IProject prj = null;
			IPath parent = location;
			while (parent.segmentCount() > 0) {
				parent = parent.removeLastSegments(1);
				if (parent.append(".tern-project").toFile().exists()) {
					IContainer container = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(parent);
					if (container != null) {
						if (container instanceof IProject) {
							prj = (IProject)container;
							break;
						}
					}
				}
			}
			IProject ternProject = (IProject)project.getAdapter(IProject.class);
			if (ternProject.equals(prj)) {
				IPath p = ternProject.getLocation();
				IPath f = iFile.getLocation();
				if (p.isPrefixOf(f)) {
					String s =  f.removeFirstSegments(f.matchingFirstSegments(p)).toString();
					return s;
				}
			}
			return PROJECT_PROTOCOL + iFile.getFullPath().makeRelative().toString();
		}
	}
	
	
	
	@Override
	public String getFileName() {
		return iFile.getName();
	}
	
	@Override
	public String getFileExtension() {
		return iFile.getFileExtension();
	}
	
	@Override
	public ITernFile getRelativeFile(String relativePath) {
		IContainer parent = iFile.getParent();
		IFile relativeFile = parent.getFile(new Path(relativePath));
		if (relativeFile != null && relativeFile.exists()) {
			return TernResourcesManager.getTernFile(relativeFile);
		}
		File rf = new File(parent.getLocation().toFile(), relativePath);
		if (rf.isFile()) {
			return TernResourcesManager.getTernFile(rf);
		}
		return null;
	}
	
	@Override
	public String getContents() throws IOException {
		try {
			IDocument doc = getDocument();
			if (doc != null) {
				return doc.get();
			}
			InputStream input = iFile.getContents();
			try {
				return IOUtils.toString(input, iFile.getCharset());
			} finally {
				input.close();
			}
		} catch (CoreException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapterClass) {
		if (adapterClass == IFile.class || adapterClass == IResource.class) {
			return iFile;
		}
		if (adapterClass == IDocument.class) {
			return getDocument();
		}
		if (adapterClass == File.class) {
			IPath path = iFile.getLocation();
			if (path != null) {
				return path.toFile();
			}
		}
		return null;
	}
	
	protected IDocument getDocument() {
		if (!docLoaded) {
			docLoaded = true;
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			IPath location = iFile.getFullPath();
			ITextFileBuffer buffer = manager.getTextFileBuffer(location,
					LocationKind.IFILE);
			if (buffer != null ) {
				this.document = buffer.getDocument();
			}
		}
		return this.document;
	}
	
	@Override
	public String toString() {
		return iFile.getFullPath().toString();
	}

	@Override
	public boolean isAccessible() {
		return iFile.isAccessible();
	}
	
	public IFile getFile() {
		return iFile;
	}
}
