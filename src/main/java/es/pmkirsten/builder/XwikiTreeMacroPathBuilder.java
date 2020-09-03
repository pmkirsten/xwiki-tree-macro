package es.pmkirsten.builder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

public class XwikiTreeMacroPathBuilder {

	private int initialCount = 0;
	private int count = 0;
	private LinkedHashSet<String> ignoreElements = new LinkedHashSet<>();
	private LinkedHashSet<String> ignoreExtensions = new LinkedHashSet<>();
	private DefaultMutableTreeNode modelTree;

	public DefaultMutableTreeNode getModelTree() {
		return this.modelTree;
	}

	public void setModelTree(DefaultMutableTreeNode modelTree) {
		this.modelTree = modelTree;
	}

	public LinkedHashSet<String> getIgnoreElements() {
		return this.ignoreElements;
	}

	public void setIgnoreElements(LinkedHashSet<String> ignoreElements) {
		this.ignoreElements = ignoreElements;
	}

	public LinkedHashSet<String> getIgnoreExtensions() {
		return this.ignoreExtensions;
	}

	public void setIgnoreExtensions(LinkedHashSet<String> ignoreExtensions) {
		this.ignoreExtensions = ignoreExtensions;
	}

	public int getInitialCount() {
		return this.initialCount;
	}

	public void setInitialCount(int initialCount) {
		this.initialCount = initialCount;
	}

	public int getCount() {
		return this.count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String walk(String myPath) {
		Path path = Paths.get(myPath);
		this.setInitialCount(path.getNameCount() - 1);
		StringBuilder builder = new StringBuilder();
		builder.append("{{wrapper}}\n|(((\n{{tree}}\n{{velocity}}\n{{html}}\n<ul>\n");
		builder.append(this.printElement(path));
		builder.append("</ul>\n{{/html}}\n{{/velocity}}\n{{/tree}}\n)))|(((\n)))\n{{/wrapper}}");
		this.setModelTree(this.buildTreeModel(path));
		return builder.toString();
	}

	public void checkGitignores(Path p) {
		List<File> gitignores = this.searchGitignores(p);
		for (File gitignore : gitignores) {
			try {
				List<String> allLines = Files.readAllLines(gitignore.toPath());
				for (String line : allLines) {
					if (line.startsWith("#") || line.isEmpty()) {
						continue;
					}

					if (line.endsWith("/")) {
						line = line.replace("/", "");
					}

					if (line.contains("/") && !line.endsWith("/")) {
						line = line.substring(line.lastIndexOf("/") + 1, line.length());
					}

					if (line.startsWith("*")) {
						this.getIgnoreExtensions().add(line.substring(1));
					}

					this.getIgnoreElements().add(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public List<File> searchGitignores(Path p) {
		ArrayList<File> list = new ArrayList<>();

		if (p.toFile().isFile() && p.getFileName().toString().equals(".gitignore")) {
			list.add(p.toFile());
			return list;
		}

		if (this.checkDirectory(p)) {
			for (File children : p.toFile().listFiles()) {
				list.addAll(this.searchGitignores(children.toPath()));
			}
		}

		return list;
	}

	public boolean checkDirectory(Path p) {
		return p.toFile().isDirectory();
	}

	public DefaultMutableTreeNode buildTreeModel(Path p) {

		DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(p.getFileName());
		if (this.checkDirectory(p)) {
			for (File children : p.toFile().listFiles()) {
				if (!this.getIgnoreElements().contains(p.getFileName().toString())) {
					DefaultMutableTreeNode childNode = this.buildTreeModel(children.toPath());
					treeNode.add(childNode);
				}
			}
		}

		return treeNode;
	}

	public String printElement(Path p) {
		if (this.getIgnoreElements().contains(p.getFileName().toString())) {
			return "";
		}
		this.setCount(p.getNameCount());
		int whiteSpaces = this.getCount() - this.getInitialCount();
		StringBuilder builder = new StringBuilder();
		if (!this.checkDirectory(p)) {
			for (String extension : this.getIgnoreExtensions()) {
				if (p.getFileName().toString().endsWith(extension)) {
					return "";
				}
			}
			builder.append(this.returnWhitespaces(whiteSpaces));
			builder.append("<li data-jstree='{\"icon\":\"glyphicon glyphicon-file\"}'>");
			builder.append(p.getFileName());
			builder.append("</li>\n");
			return builder.toString();
		}

		if (this.checkDirectory(p) && (p.toFile().list().length == 0)) {
			builder.append(this.returnWhitespaces(whiteSpaces));
			builder.append("<li data-jstree='{\"icon\":\"glyphicon glyphicon-folder-open\"}'>");
			builder.append(p.getFileName());
			builder.append("</li>\n");
			return builder.toString();
		}

		builder.append(this.returnWhitespaces(whiteSpaces));
		builder.append("<li data-jstree='{");
		if ((this.getCount() - this.getInitialCount() - 1) == 0) {
			builder.append("\"opened\":true, ");
		}
		builder.append("\"icon\":\"glyphicon glyphicon-folder-open\"}'>\n");
		builder.append(this.returnWhitespaces(whiteSpaces));
		builder.append(p.getFileName() + "\n");
		builder.append(this.returnWhitespaces(whiteSpaces));
		builder.append("<ul>\n");
		List<File> folders = new ArrayList<>();
		List<File> files = new ArrayList<>();
		for (File children : p.toFile().listFiles()) {
			if (this.checkDirectory(children.toPath())) {
				folders.add(children);
			} else {
				files.add(children);
			}
		}
		folders.addAll(files);
		for (File children : folders) {
			builder.append(this.printElement(children.toPath()));
		}
		this.setCount(p.getNameCount());
		builder.append(this.returnWhitespaces(whiteSpaces));
		builder.append("</ul>\n");
		builder.append(this.returnWhitespaces(whiteSpaces));
		builder.append("</li>\n");

		return builder.toString();

	}

	public String returnWhitespaces(int times) {
		return String.join("", Collections.nCopies(times, "  "));
	}

	public static void main(String[] args) {
		XwikiTreeMacroPathBuilder builder = new XwikiTreeMacroPathBuilder();
		String myPath = "F:\\workspace\\RaceControl\\src";
		builder.getIgnoreElements().add(".git");
		builder.checkGitignores(Paths.get(myPath));
		System.out.println(builder.walk(myPath));
	}
}