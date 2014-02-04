package tern.angular.protocol;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import tern.angular.AngularType;
import tern.angular.modules.Directive;
import tern.angular.modules.IDirectiveProvider;

public class HTMLTernAngularHelper {

	public static void populateScope(Node element, IDirectiveProvider provider,
			TernAngularQuery query) {
		TernAngularScope scope = query.getScope();
		populateScope(element, scope, provider, query.getFirstAngularType());
	}

	public static void populateScope(Node element, TernAngularScope scope,
			IDirectiveProvider provider, AngularType angularType) {
		switch (angularType) {
		case module:
			// do nothing;
			break;
		case controller:
			// find controller
			populateScope(scope, element, provider, false);
			break;
		case model:
		case directive:
		case directiveRepeat:
			// find model
			populateScope(scope, element, provider, true);
			break;
		}

	}

	private static void populateScope(TernAngularScope scope, Node element,
			IDirectiveProvider provider, boolean populateController) {
		if (element == null || element.getNodeType() == Node.DOCUMENT_NODE) {
			return;
		}
		NamedNodeMap attributes = element.getAttributes();
		if (attributes != null) {
			Attr node = null;
			for (int i = 0; i < attributes.getLength(); i++) {
				node = (Attr) attributes.item(i);
				Directive directive = provider.getAngularDirective(node);
				if (directive != null) {
					switch (directive.getType()) {
					case module:
						String module = ((Attr) node).getValue();
						scope.setModule(module);
						return;
					case controller:
						if (populateController) {
							String controller = ((Attr) node).getValue();
							scope.addController(controller);
						}
						break;
					case model:
						String model = ((Attr) node).getValue();
						scope.addModel(model);
						break;
					case directiveRepeat:
						String expression = ((Attr) node).getValue();
						scope.addRepeat(expression);
						break;
					default:
						break;
					}
				}
			}
		}
		Node parent = element.getPreviousSibling();
		if (parent == null)
			parent = element.getParentNode();
		populateScope(scope, parent, provider, populateController);
	}

}