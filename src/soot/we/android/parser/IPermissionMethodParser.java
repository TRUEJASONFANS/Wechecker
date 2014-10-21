
package soot.we.android.parser;

import java.io.IOException;
import java.util.Set;

import soot.we.android.callGraph.AndroidMethod;

/**
 * Common interface for all parsers that are able to read in files with Android
 * methods and permissions
 * 
 * 
 */
public interface IPermissionMethodParser {

	Set<AndroidMethod> parse() throws IOException;

}
