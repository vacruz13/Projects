package gitlet;

import java.io.File;
import java.io.Serializable;

/**
 * Blob file holds file object.
 * @author victorcruz
 */
public class Blob implements Serializable {
    static final File BLOB_FOLDER = new File(Main.GITLET_DIR, "blobs");

    /**
     Constructor.
     */
    public Blob(File file) {
        _contents = Utils.readContentsAsString(file);
    }
    /**
     For _contents retrieval.
     */
    public String getContents() {
        return _contents;
    }
    /**
     _contents contains file contents.
     */
    private String _contents;
}
