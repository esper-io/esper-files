package io.esper.files.model;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;

import io.esper.files.R;

/**
 * Model object for a file item.
 */
public class FileItem implements Comparable<FileItem> {

    private String name;
    private String path;
    private String pathUri;
    private Boolean isDirectory;

    private Boolean isImage;

    private Boolean isSpecial = Boolean.FALSE;

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
        this.pathUri = Uri.fromFile(new File(path)).toString();
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
    public boolean equals(Object other){
        return !(other == null || !(other instanceof FileItem)) &&
                this.getPath().equals(((FileItem)other).getPath());
    }

    public boolean couldHaveThumbnail(){
        return !(isDirectory || isSpecial);
    }

    public Boolean getIsImage() {
        return isImage;
    }

    public void setIsImage(Boolean isImage) {
        this.isImage = isImage;
    }

    public boolean getIsSpecial() {
        return isSpecial;
    }

    public void setIsSpecial() {
        this.isSpecial = true;
    }

}