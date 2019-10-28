/**
 * 
 */
package com.kevinguanchedarias.owgejava.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;

/**
 * Has methods related with the game repository
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class GitUtilService {
	private static final String GIT_REPOSITORY = "https://github.com/KevinGuancheDarias/owge";

	@Autowired
	private MavenUtilService mavenUtilService;

	/**
	 * Returns the github URL for the given doc file
	 * 
	 * @param repository Github repository. Ie
	 *                   https://github.com/KevinGuancheDarias/owge
	 * @param project    The target git project
	 * @param clazz      Class to use as name (Usually the class that throws the
	 *                   exception)
	 * @param docsPath   the docs path because it's a custom doc
	 * @param doc        the document path (for example exceptions/invalid_type.md)
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String createDocUrl(String repository, GameProjectsEnum project, Class<?> clazz, DocTypeEnum docsPath,
			String doc) {
		if (docsPath == DocTypeEnum.EXCEPTIONS && !doc.startsWith("I18N_ERR")) {
			throw new ProgrammingException("When the doc is an exception, doc MUST start by I18N_ERR");
		}
		return repository + "/" + "blob/" + findVersionString() + "/" + project.getProjectPath() + "/docs/"
				+ clazz.getName() + "/" + docsPath.findPath() + "/" + doc.toLowerCase() + ".md";
	}

	/**
	 * Returns the github URL for the given doc file
	 * 
	 * @param project  The target git project
	 * @param clazz    Class to use as name (Usually the class that throws the
	 *                 exception)
	 * @param docsPath the docs path because it's a custom doc
	 * @param doc      the document path (for example exceptions/invalid_type.md)
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String createDocUrl(GameProjectsEnum project, Class<?> clazz, DocTypeEnum docsPath, String doc) {
		return createDocUrl(GIT_REPOSITORY, project, clazz, docsPath, doc);
	}

	private String findVersionString() {
		String version = mavenUtilService.findVersion("master");
		return version.matches("^[0-9].*$") ? "v" + version : "master";
	}
}
