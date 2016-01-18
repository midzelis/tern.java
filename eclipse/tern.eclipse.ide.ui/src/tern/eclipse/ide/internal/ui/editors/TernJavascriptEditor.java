package tern.eclipse.ide.internal.ui.editors;


import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import tern.eclipse.ide.core.resources.TernDocumentFile;
import tern.eclipse.ide.internal.ui.views.TernContentOutlinePage;
import tern.eclipse.ide.ui.utils.EditorUtils;

/**
 * Provide a better ouline for javascript
 * 
 * @author nkruk
 *
 */
public class TernJavascriptEditor extends CompilationUnitEditor {

	@Override
	public Object getAdapter(Class required) {
		if (IContentOutlinePage.class.equals(required)) {
			IFile file = EditorUtils.getFile(this);
			TernContentOutlinePage page = new TernContentOutlinePage( null);
			page.setCurrentFile(file);
			return page;
		}
		
		return super.getAdapter(required);
	}
}

