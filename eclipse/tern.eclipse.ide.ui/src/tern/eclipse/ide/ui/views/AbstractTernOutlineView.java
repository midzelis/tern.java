/**
 *  Copyright (c) 2013-2015 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package tern.eclipse.ide.ui.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import tern.eclipse.ide.internal.ui.TernUIMessages;
import tern.eclipse.ide.ui.utils.EditorUtils;
import tern.server.protocol.outline.IJSNode;

/**
 * Abstract class for tern outline view.
 *
 */
public abstract class AbstractTernOutlineView extends ContentOutline implements ISelectionChangedListener {

	public static final int IS_LINKING_ENABLED_PROPERTY = 0;

	private ITextEditor textEditor;

	protected IMemento memento;

	private String LINKING_ENABLED = "AbstractTernOutlineView.LINKING_ENABLED"; //$NON-NLS-1$
	private boolean linkingEnabled = false;
	private ISelection currentEditorSelection;


	private class BestNode {
		public final IJSNode node;
		public final CommonViewer viewer;

		public BestNode(IJSNode node, CommonViewer viewer) {
			this.node = node;
			this.viewer = viewer;
		}
	}

	private Job updateSelectionJob = new Job(TernUIMessages.Link_With_Editor_Job_) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			BestNode bestNode = findBestNode(currentEditorSelection);
			if (bestNode != null) {
				final IJSNode node = bestNode.node;
				final CommonViewer viewer = bestNode.viewer;
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						viewer.setSelection(new StructuredSelection(node));
					}
				});
			}
			return Status.OK_STATUS;
		}
	};

	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		IFile file = getFile(part);
		if (file == null) {
			// The opened file in the given editor cannot be displayed in the
			// outline
			return null;
		}

		IContentOutlinePage page = getOutlinePage(part, file);
		if (page == null) {
			// Try to get an outline page.
			page = createOutlinePage(part, file);
			if (page != null) {
				if (page instanceof IPageBookViewPage) {
					initPage((IPageBookViewPage) page);
				}
				if (page instanceof AbstractTernContentOutlinePage) {
					((AbstractTernContentOutlinePage) page).setCurrentFile(file);
					this.setCurrentPart(part);
				}
				page.createControl(getPageBook());
			}
		} else {
			if (page instanceof AbstractTernContentOutlinePage) {
				((AbstractTernContentOutlinePage) page).setCurrentFile(file);
				this.setCurrentPart(part);
			}
		}

		if (page != null) {
			return new PageRec(part, page);
		}

		// There is no content outline
		return null;
	}

	@Override
	protected void showPageRec(PageRec pageRec) {
		IPage page = pageRec.page;
		IWorkbenchPart part = pageRec.part;
		if (page instanceof AbstractTernContentOutlinePage) {
			IFile file = getFile(part);
			if (file != null) {
				((AbstractTernContentOutlinePage) page).setCurrentFile(file);
				this.setCurrentPart(part);
			}
		}
		super.showPageRec(pageRec);
	}

	protected IContentOutlinePage getOutlinePage(IWorkbenchPart part, IFile file) {
		return null;
	}

	protected IFile getFile(IWorkbenchPart part) {
		if (part != null && part instanceof IEditorPart) {
			IFile file = EditorUtils.getFile((IEditorPart) part);
			if (file != null && isAdaptFor(file)) {
				return file;
			}
		}
		return null;
	}

	@Override
	public void init(IViewSite aSite, IMemento aMemento) throws PartInitException {
		super.init(aSite, aMemento);
		memento = aMemento;
		if (memento != null) {
			Integer linkingEnabledInteger = memento.getInteger(LINKING_ENABLED);
			setLinkingEnabled(((linkingEnabledInteger != null) ? linkingEnabledInteger.intValue() == 1 : false));
		}

	}

	@Override
	public void saveState(IMemento aMemento) {
		aMemento.putInteger(LINKING_ENABLED, (linkingEnabled) ? 1 : 0);
		super.saveState(aMemento);
	}

	/**
	 * Returns true if the outline view is adapted for the given file and false
	 * otherwise.
	 * 
	 * @param file
	 * @return true if the outline view is adapted for the given file and false
	 *         otherwise.
	 */
	protected abstract boolean isAdaptFor(IFile file);

	/**
	 * Create the outline view for the given file.
	 * 
	 * @param part
	 * 
	 * @param file
	 * @param view
	 * @return the outline view for the given file.
	 */
	protected abstract IContentOutlinePage createOutlinePage(IWorkbenchPart part, IFile file);

	public boolean isLinkingEnabled() {
		return linkingEnabled;
	}

	public void setLinkingEnabled(boolean linkingEnabled) {
		this.linkingEnabled = linkingEnabled;
	}

	public void setCurrentPart(IWorkbenchPart part) {
		if (this.textEditor != null) {
			uninstall(this.textEditor.getSelectionProvider());
		}
		if (part instanceof ITextEditor) {
			this.textEditor = (ITextEditor) part;
			ISelectionProvider provider = this.textEditor.getSelectionProvider();
			this.currentEditorSelection = provider.getSelection();
			updateSelectionJob.schedule(10);
			install(provider);
		} else {
			this.textEditor = null;
		}
	}

	private void install(ISelectionProvider selectionProvider) {
		if (selectionProvider == null) {
			return;
		}
		if (selectionProvider instanceof IPostSelectionProvider) {
			IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
			provider.addPostSelectionChangedListener(this);
		} else {
			selectionProvider.addSelectionChangedListener(this);
		}
	}

	private void uninstall(ISelectionProvider selectionProvider) {
		if (selectionProvider == null) {
			return;
		}
		if (selectionProvider instanceof IPostSelectionProvider) {
			IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
			provider.removePostSelectionChangedListener(this);
		} else {
			selectionProvider.removeSelectionChangedListener(this);
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		this.currentEditorSelection = event.getSelection();
		updateSelectionJob.schedule(10);
	}

	private BestNode findBestNode(ISelection selection) {
		if (!this.isLinkingEnabled() /* && !ignoreSelectionChanged */ ) {
			return null;
		}
		CommonViewer viewer = getCurrentViewer();
		IStructuredContentProvider contentProvider = (IStructuredContentProvider) viewer.getContentProvider();
		if (contentProvider == null || selection == null || selection.isEmpty()
				|| !(selection instanceof ITextSelection)) {
			return null;
		}
		Object[] elements = contentProvider.getElements(viewer.getInput());
		if (elements != null) {
			Object elt = null;
			IJSNode bestNode = null;
			for (int i = 0; i < elements.length; i++) {
				elt = elements[i];
				if (elt instanceof IJSNode) {
					bestNode = findBestNode((IJSNode) elt, (ITextSelection) selection);
					if (bestNode != null) {
						return new BestNode(bestNode, viewer);
					}
				}
			}
		}
		return null;
	}

	protected CommonViewer getCurrentViewer() {
		IPage p = getCurrentPage();
		if (p == null || !(p instanceof AbstractTernContentOutlinePage)) {
			return null;
		}
		AbstractTernContentOutlinePage page = (AbstractTernContentOutlinePage) p;
		CommonViewer viewer = page.getViewer();
		if (viewer == null) {
			return null;
		}
		return viewer;
	}

	private IJSNode findBestNode(IJSNode node, ITextSelection selection) {
		int start = selection.getOffset(), end = start + selection.getLength();
		if (node.getStart() != null) {
			if (node.getStart() <= start && node.getEnd() >= end) {
				return node;
			}
		}
		if (node.hasChidren()) {
			for (IJSNode child : node.getChildren()) {
				IJSNode c = findBestNode(child, selection);
				if (c != null) {
					return c;
				}
			}
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (this.textEditor != null) {
			uninstall(this.textEditor.getSelectionProvider());
		}
	}
}
