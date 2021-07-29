package io.esper.files.model;


import androidx.annotation.NonNull;

/**
 * Model object for a file item.
 */
public class FileItem implements Comparable<FileItem> {

    private String name;
    private String path;
    private Boolean isDirectory;

    private Boolean isImage;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getIsDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(Boolean directory) {
        isDirectory = directory;
    }

    @Override
    public int compareTo(@NonNull FileItem other) {
        if (this.getIsDirectory() == other.getIsDirectory()) {
            return this.getPath().compareToIgnoreCase(other.getPath());
        } else {
            return this.getIsDirectory() ? -1 : 1;
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FileItem &&
                this.getPath().equals(((FileItem) other).getPath());
    }

    public Boolean getIsImage() {
        return isImage;
    }

    public void setIsImage(Boolean isImage) {
        this.isImage = isImage;
    }

}