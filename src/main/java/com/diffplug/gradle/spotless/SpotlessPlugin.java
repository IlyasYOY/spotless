/**
 * Copyright 2015 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.spotless;

import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaBasePlugin;

public class SpotlessPlugin implements Plugin<Project> {
	Project project;
	SpotlessExtension extension;

	static final String EXTENSION = "spotless";
	static final String CHECK = "Check";
	static final String APPLY = "Apply";

	public void apply(Project project) {
		this.project = project;

		// setup the extension
		extension = project.getExtensions().create(EXTENSION, SpotlessExtension.class, project);
		// ExtensionContainer container = ((ExtensionAware) project.getExtensions().getByName(EXTENSION)).getExtensions();

		// after the project has been evaluated, configure the check and format tasks per source set
		project.afterEvaluate(unused -> {
			try {
				createTasks();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	/** The extension for this plugin. */
	public SpotlessExtension getExtension() {
		return extension;
	}

	void createTasks() throws Exception {
		Task rootCheckTask = project.task(EXTENSION + CHECK);
		Task rootApplyTask = project.task(EXTENSION + APPLY);

		for (Map.Entry<String, FormatExtension> entry : extension.formats.entrySet()) {
			rootCheckTask.dependsOn(createTask(entry.getKey(), entry.getValue(), true));
			rootApplyTask.dependsOn(createTask(entry.getKey(), entry.getValue(), false));
		}

		// add the check task as a dependency to the global check task (if there is one)
		Task checkTask = project.getTasks().getAsMap().get(JavaBasePlugin.CHECK_TASK_NAME);
		if (checkTask != null) {
			checkTask.dependsOn(rootCheckTask);
		}
	}

	FormatTask createTask(String name, FormatExtension subExtension, boolean check) throws Exception {
		FormatTask task = project.getTasks().create(EXTENSION + capitalize(name) + (check ? CHECK : APPLY), FormatTask.class);
		task.lineEndings = extension.lineEndings;
		task.check = check;
		// sets toFormat and steps
		subExtension.setupTask(task);
		return task;
	}

	private static String capitalize(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
}