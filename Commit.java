package gitlet;
import java.io.Serializable;
import java.util.*;
import java.time.*;
import java.io.File;
/**
 Commits hold very important information about the state of the repository at a given time.
 @author victorcruz
 */

public class Commit implements Serializable {
    /** Folder that commits live in.*/
    static final File COMMITS_FOLDER = new File(Main.GITLET_DIR,"commits");
    private String _message;

    private ZonedDateTime _timestamp;
    private String _parent;
    /**
     Datastructure with:
     Key: filename.
     Value: blob hash.

     */
    private LinkedHashMap <String, String> _blobs;

    /**
     Constructor.
     */
    public Commit(String message, String parent, LinkedHashMap <String, String> blob, ZonedDateTime time) {
            _message = message;
            _parent = parent;
            _timestamp = time;
            _blobs = blob;
    }
    /**
     Change message after cloning last commit.
     */
    public void changeMessage(String newMessage) {
        _message = newMessage;
    }
    /**
     Change parent pointer after cloning last commit.
     */
    public void changeParent(String newParent) {
        _parent = newParent;
    }
    /**
     Change file pointers after cloning last commit.
     */
    public void add_replaceBlob(String fileName, String blob) {
        if (_blobs.containsKey(fileName)) {
            _blobs.replace(fileName, blob);
        } else {
            _blobs.put(fileName, blob);
        }
    }
    /**
     Change timestamp after cloning last commit.
     */
    public void changeTime(ZonedDateTime newTime) {
        _timestamp = newTime;
    }
    /**
     Removing blobs is useful during commits.
     */
    public void removeBlob(String file_name) {
        _blobs.remove(file_name);
    }
    /**
     Returns a copy of this commit.
     */
    public Commit clone() {
        return new Commit(getMessage(), getParent(), getBlobs(), getTimeStamp());
    }
    /**
     Retrieve message.
     */
    public String getMessage() {
        return _message;
    }
    /**
     Retrieve time.
     */
    public ZonedDateTime getTimeStamp() {
        return _timestamp;
    }
    /**
     Retrive parent pointer
     */
    public String getParent() {
        return _parent;
    }
    /**
     Retrieve file pointers.
     */
    public LinkedHashMap <String, String> getBlobs() {
        return new LinkedHashMap<>(_blobs);
    }
}
