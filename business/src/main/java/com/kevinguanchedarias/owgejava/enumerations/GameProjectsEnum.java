/**
 * 
 */
package com.kevinguanchedarias.owgejava.enumerations;

/**
 * Has the possible sub-folders for the root of the repository
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public enum GameProjectsEnum {
	BUSINESS("business"), REST("game-rest");

	private final String projectPath;

	/**
	 * @param projectPath Folder inside the git repository root
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private GameProjectsEnum(final String projectPath) {
		this.projectPath = projectPath;
	}

	/**
	 * Folder inside the git repository root
	 * 
	 * @since 0.8.0
	 * @return the projectPath
	 */
	public String getProjectPath() {
		return projectPath;
	}

}
