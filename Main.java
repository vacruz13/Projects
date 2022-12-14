package gitlet;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.*;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author victorcruz
 */
public class Main {
    /** Current Working Directory. */
    static final File CWD = new File(".");

    /** Main metadata folder. */
    static final File GITLET_DIR = new File(CWD, ".gitlet") ;

    static final File StagingArea = new File(GITLET_DIR, "StagingArea");

    static final File filesForRemove = new File(GITLET_DIR, "RemoveFiles");

    static final File BRANCHES_DIR = new File(GITLET_DIR,"branches");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        // FILL THIS IN
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        switch (args[0]) {
            case "init":
                validateNumArgs("init", args,1);
                init();
                break;
            case "add":
                validateNumArgs("add", args, 2);
                checkInit();
                add(args);
                break;
            case "commit":
                validateNumArgs("commit", args, 2);
                checkInit();
                commit(args);
                break;
            case "checkout":
                checkInit();
                checkout(args);
                break;
            case "rm":
                validateNumArgs("rm", args, 2);
                checkInit();
                rm(args);
                break;
            case "status":
                validateNumArgs("status", args, 1);
                checkInit();
                status();
                break;
            case "log":
                validateNumArgs("log", args,1);
                checkInit();
                log(args);
                break;
            case "global-log":
                validateNumArgs("global-log", args,1);
                checkInit();
                globalLog();
                break;
            case "branch":
                validateNumArgs("branch", args, 2);
                checkInit();
                branch(args);
                break;
            case "find":
                validateNumArgs("find", args, 2);
                checkInit();
                find(args);
                break;
            case "rm-branch":
                validateNumArgs("rm-branch", args, 2);
                checkInit();
                rmBranch(args);
                break;
            case "reset":
                validateNumArgs("rest", args, 2);
                checkInit();
                reset(args);
                break;
            case "merge":
                validateNumArgs("merge", args, 2);
                checkInit();
                merge(args);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }
    /**
     * Does required filesystem operations to allow for persistence.
     * (creates any necessary folders or files)
     *
     * .gitlet/ -- top level folder for all persistent data in your lab12 folder
     *    - commits/ -- folder containing all of the persistent data for dogs
     *    - blobs -- file containing the current story
     *
     */
    public static void init() {
        // FIXME
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
            Blob.BLOB_FOLDER.mkdir();
            Commit.COMMITS_FOLDER.mkdir();
            Commit initialCommit = new Commit("initial commit", null, new LinkedHashMap<>(), LocalDateTime.of(1969, 12, 31, 16, 0, 0).atZone(ZoneId.of("America/Los_Angeles")));

            File commitFile = new File(Commit.COMMITS_FOLDER, findCommitHash(initialCommit));
            Utils.writeObject(commitFile, initialCommit);
            BRANCHES_DIR.mkdir();
            File initialHead = new File(GITLET_DIR, "HEAD");
            Utils.writeContents(initialHead, "master");
            moveHead(initialCommit);
        } else {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
    }

    /**  Adds a copy of the file as it currently exists to the staging area
     *
     */
    public static void add(String[] args) {
            File add = new File(CWD, args[1]);
            Blob addBlob = new Blob(add);
            LinkedHashMap<String, String> headCommitBlobs = getHeadCommit().getBlobs();
            if (add.exists()) {
                stageOverwrite(filesForRemove, args[1]);
                if (headCommitBlobs.containsKey(args[1])) {
                    if (headCommitBlobs.get(args[1]).equals(findBlobHash(addBlob))) {
                        stageOverwrite(StagingArea, args[1]);
                    } else {
                        File addFile = new File(Blob.BLOB_FOLDER, findBlobHash(addBlob));
                        Utils.writeObject(addFile, addBlob);
                        stageFile(findBlobHash(addBlob), args[1]);
                    }
                } else {
                    File addFile = new File(Blob.BLOB_FOLDER, findBlobHash(addBlob));
                    Utils.writeObject(addFile, addBlob);
                    stageFile(findBlobHash(addBlob), args[1]);
                }
            } else {
                System.out.println("File does not exist.");
                System.exit(0);
            }
        }

    /**
     * Saves a snapshot of tracked files in the current commit
     * and staging area so they can be restored at a later time, creating a new commit.
     */
    public static void commit(String[] args) {
        Commit headClone = getHeadCommit().clone();
        headClone.changeMessage(args[1]);
        headClone.changeTime(ZonedDateTime.now());
        if (args[1] == "" || args[1] == null) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        List<String> stageFiles = Utils.plainFilenamesIn(StagingArea);
        if(stageFiles == null) {
            System.out.println("Nothing to commit");
            System.exit(0);
        }
        for (String file_name: stageFiles) {
            File thisFile = new File(StagingArea, file_name);
            headClone.add_replaceBlob(file_name, Utils.readContentsAsString(thisFile));
            thisFile.delete();
        }
        List<String> removeFiles = Utils.plainFilenamesIn(filesForRemove);
        if (removeFiles!=null) {
            for (String s: removeFiles) {
                headClone.removeBlob(s);
                File thisFile = new File(filesForRemove, s);
                thisFile.delete();
            }
        }
        headClone.changeParent(findCommitHash(getHeadCommit()));
        File commitFile = new File(Commit.COMMITS_FOLDER, findCommitHash(headClone));
        Utils.writeObject(commitFile, headClone);
        moveHead(headClone);
    }

    public static void checkout(String [] args) {
        if(args[1].equals("--") && args.length == 3) {
            if (getHeadCommit().getBlobs().containsKey(args[2])) {
                File presentFile = new File(CWD, args[2]);
                if (presentFile.exists()) {
                    presentFile.delete();
                }
                String savedFileHash = getHeadCommit().getBlobs().get(args[2]);
                File blobFile = new File(Blob.BLOB_FOLDER,savedFileHash);
                if (blobFile.exists()) {
                    Utils.writeContents(presentFile, Utils.readObject(blobFile, Blob.class).getContents());
                }
            } else {
                System.out.println("File does not exist in that commit");
                System.exit(0);
            }
        }
        else if (args.length == 4 && args[2].equals("--")) {
            Commit thisCommit = getCommit(args[1]);
            if (thisCommit == null) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            if (thisCommit.getBlobs().get(args[3]) == null) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
            String savedFileHash = thisCommit.getBlobs().get(args[3]);
            File blobFile = new File(Blob.BLOB_FOLDER,savedFileHash);
            File presentFile = new File(CWD, args[3]);
            if (presentFile.exists()) {
                presentFile.delete();
            }
            Utils.writeContents(presentFile, Utils.readObject(blobFile, Blob.class).getContents());
        }
        else if (args.length == 2) {
            File headFile = new File(GITLET_DIR, "HEAD");
            File newBranch = new File(BRANCHES_DIR, args[1]);
            List <String> stageSize = Utils.plainFilenamesIn(StagingArea);
            int untrackedFilesSize = getUntrackedFiles().size();
            if (stageSize == null) {
                if (untrackedFilesSize > 0) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
            else if (untrackedFilesSize > 0  || stageSize.size() > 0) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
            if (!newBranch.exists()) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
            if (Utils.readContentsAsString(headFile).equals(args[1])) {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }

            Commit oldHEAD = getHeadCommit();
            headFile.delete();
            Utils.writeContents(headFile, args[1]);

            oldHEAD.getBlobs().keySet().forEach(key -> {
                if (!getHeadCommit().getBlobs().containsKey(key)) {
                    File deleteFile = new File(CWD, key);
                    if (deleteFile.exists()) {

                        deleteFile.delete();
                    }
                }
            });

            getHeadCommit().getBlobs().entrySet().forEach( entry -> {
                File thisFile = new File (CWD, entry.getKey());
                if (thisFile.exists()) {
                    thisFile.delete();
                }
                File thisBlob = new File(Blob.BLOB_FOLDER, entry.getValue());
                Utils.writeContents(thisFile, Utils.readObject(thisBlob, Blob.class).getContents());

            });

            List<String> stageFiles = Utils.plainFilenamesIn(StagingArea);
            if (stageFiles != null) {
                for (String file_name: stageFiles) {
                    File thisFile = new File(StagingArea, file_name);
                    thisFile.delete();
                }
            }
        }

    }

    public static void log(String[] args) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy Z");
        Commit start = getHeadCommit();
        while(true) {
            System.out.println("===");
            System.out.println("commit " + findCommitHash(start));
            System.out.println("Date: " + start.getTimeStamp().format(formatter));
            System.out.println(start.getMessage() + "\n");
            if (start.getParent() == null) {
                break;
            }
            start = getCommit(start.getParent());
        }
    }

    public static void globalLog() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy Z");
        List<String> commits = Utils.plainFilenamesIn(Commit.COMMITS_FOLDER);
        for (String commit: commits) {
            Commit thisCommit = getCommit(commit);
            System.out.println("===");
            System.out.println("commit " + commit);
            System.out.println("Date: " + thisCommit.getTimeStamp().format(formatter));
            System.out.println(thisCommit.getMessage() + "\n");
        }
    }

    public static void branch(String [] args) {
        File newBranch = new File(BRANCHES_DIR, args[1]);
        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        Utils.writeContents(newBranch, getHead());
    }

    public static void status() {
        System.out.println("=== Branches ====");
        File headFile = new File(GITLET_DIR, "HEAD");
        String activeBranch = Utils.readContentsAsString(headFile);
        List <String> branches = Utils.plainFilenamesIn(BRANCHES_DIR);
        for (String branch: branches) {
            if (branch.equals(activeBranch)) {
                System.out.println("*"+activeBranch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        List <String> stagedFiles = Utils.plainFilenamesIn(StagingArea);
        if (stagedFiles != null) {
            for (String file: stagedFiles) {
                System.out.println(file);
            }
        }
        System.out.println("");
        System.out.println("=== Removed Files ===");
        List <String> removeFiles = Utils.plainFilenamesIn(filesForRemove);
        if (removeFiles != null) {
            for (String file: removeFiles) {
                System.out.println(file);
            }
        }
        System.out.println("");
        System.out.println("=== Modifications Not Staged For Commit ===");
        getAndPrintMNSCFiles();
        System.out.println("");
        System.out.println("=== Untracked File ===");
        ArrayList <String> untrackedFiles = getUntrackedFiles();
        if (untrackedFiles.size() != 0) {
            for (String s: untrackedFiles) {
                System.out.println(s);
            }
        }
    }
    public static void rm(String [] args) {
        List<String> stageFiles = Utils.plainFilenamesIn(StagingArea);
        int modification = 1;
        if (stageFiles != null) {
            for (String file_name: stageFiles) {
                if (file_name.equals(args[1])) {
                    modification *= 0;
                    File thisFile = new File(StagingArea, file_name);
                    thisFile.delete();
                }
            }
        }
        if (getHeadCommit().getBlobs().containsKey(args[1])) {
            modification *= 0;
            stageRemove(getHeadCommit().getBlobs().get(args[1]), args [1]);
            File cwdFile = new File(CWD, args[1]);
            if (cwdFile.exists()) {
                cwdFile.delete();
            }
        }
        if (modification == 1) {
            System.out.println("No reason to remove the file.");
        }
    }

    public static void find(String [] args) {
        List <String> commits = Utils.plainFilenamesIn(Commit.COMMITS_FOLDER);
        int check = 1;
        for (String s: commits) {
            Commit thisCommit = getCommit(s);
            if (thisCommit.getMessage() != null) {
                if (thisCommit.getMessage().equals(args[1])) {
                    check *= 0;
                    System.out.println(s);
                }
            }
        }
        if (check == 1) {
            System.out.println("Found no commit with that message.");
        }
    }
    public static void rmBranch(String [] args) {
        File headFile = new File (GITLET_DIR, "HEAD");
        if (Utils.readContentsAsString(headFile).equals(args[1])) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        File thisBranch = new File(BRANCHES_DIR, args[1]);
        if (!thisBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else {
            thisBranch.delete();
        }
    }

    public static void reset(String[] args) {
        List <String> stageArea = Utils.plainFilenamesIn(StagingArea);
        int untrackedFilesSize = getUntrackedFiles().size();
        if (stageArea == null) {
            if (untrackedFilesSize > 0) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        else if (untrackedFilesSize > 0  || stageArea.size() > 0) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
        LinkedHashMap<String, String> trackedFiles = getHeadCommit().getBlobs();
        if (getCommit(args[1]) == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        LinkedHashMap<String, String> thisCommitFiles = getCommit(args[1]).getBlobs();
        if (thisCommitFiles != null) {
            thisCommitFiles.entrySet().forEach(entry -> {
                checkout(new String[] {"checkout", args[1], "--", entry.getKey()});
            });
        }
        trackedFiles.keySet().forEach(key -> {
            if (!thisCommitFiles.containsKey(key)) {
                File removeFile = new File(CWD, key);
                if (removeFile.exists()) {
                    removeFile.delete();
                }
            }
        });
        moveHead(getCommit(args[1]));
        if (stageArea != null) {
            for (String file_name: stageArea) {
                File thisFile = new File(StagingArea, file_name);
                thisFile.delete();
            }
        }
    }

    public static void merge(String [] args) {
        _conflictUsed = false;
        List <String> stageArea = Utils.plainFilenamesIn(StagingArea);
        List <String> removeArea = Utils.plainFilenamesIn(filesForRemove);
        int untrackedFilesSize = getUntrackedFiles().size();
        if (stageArea == null) {
            if (untrackedFilesSize > 0) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        else if (untrackedFilesSize > 0  || stageArea.size() > 0) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
        if (removeArea != null) {
                if (removeArea.size() > 0) {
                    System.out.println("You have uncommitted changes.");
                    System.exit(0);
                }
        }
        File givenBranch = new File(BRANCHES_DIR, args[1]);
        if (!givenBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        File headFile = new File(GITLET_DIR, "HEAD");
        if (args[1].equals(Utils.readContentsAsString(headFile))) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        String splitUID = getSplitPoint(args[1]);
        if (Utils.readContentsAsString(givenBranch).equals(splitUID)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (getHead().equals(splitUID)) {
            checkout(new String[] {"checkout", args[1]});
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        LinkedHashMap<String, String> splitFiles = getCommit(splitUID).getBlobs();
        LinkedHashMap<String, String> headFiles = getHeadCommit().getBlobs();
        LinkedHashMap<String, String> givenFiles = getCommit(Utils.readContentsAsString(givenBranch)).getBlobs();


        LinkedHashMap<String, String>  allFiles = getCommit(splitUID).clone().getBlobs();
        allFiles.putAll(headFiles);
        allFiles.putAll(givenFiles);

        allFiles.keySet().forEach(key -> {
            File cwdFile = new File(CWD, key);

            if (!splitFiles.containsKey(key)) {
                if (!headFiles.containsKey(key)) {
                    if (givenFiles.containsKey(key)) {
                        Blob givenBlob = getBlob(givenFiles.get(key));
                        Utils.writeContents(cwdFile, givenBlob.getContents());
                        stageFile(givenFiles.get(key), key);
                    }
                } else {
                    Blob headBlob = getBlob(headFiles.get(key));
                    if (!givenFiles.containsKey(key)) {
                        Utils.writeContents(cwdFile, headBlob.getContents());
                    } else if (givenFiles.containsKey(key)) {
                        Blob givenBlob = getBlob(givenFiles.get(key));
                        if (!givenBlob.getContents().equals(headBlob.getContents())) {
                            Utils.writeContents(cwdFile,conflictString(headBlob, givenBlob));
                            Blob addBlob = new Blob(cwdFile);
                            File addFile = new File(Blob.BLOB_FOLDER, findBlobHash(addBlob));
                            Utils.writeObject(addFile, addBlob);
                            stageFile(findBlobHash(addBlob), key);
                        }
                    }
                }
            }
            else {
                if (headFiles.containsKey(key)) {
                    Boolean headModified = isModified(splitFiles.get(key), headFiles.get(key));
                    if (givenFiles.containsKey(key)) {
                        Boolean givenModified = isModified(splitFiles.get(key), givenFiles.get(key));
                        if(!headModified && givenModified) {
                            Utils.writeContents(cwdFile, getBlob(givenFiles.get(key)).getContents());
                            stageFile(givenFiles.get(key), key);
                        } else if (headModified && !givenModified) {
                            Utils.writeContents(cwdFile, getBlob(headFiles.get(key)).getContents());
                        } else if (givenModified && headModified) {
                            if (isModified(headFiles.get(key), givenFiles.get(key))) {
                                Blob givenBlob = getBlob(givenFiles.get(key));
                                Blob headBlob = getBlob(headFiles.get(key));
                                Utils.writeContents(cwdFile,conflictString(headBlob, givenBlob));
                                Blob addBlob = new Blob(cwdFile);
                                File addFile = new File(Blob.BLOB_FOLDER, findBlobHash(addBlob));
                                Utils.writeObject(addFile, addBlob);
                                stageFile(findBlobHash(addBlob), key);
                            }

                        }
                    } else {
                        if (headModified) {
                            Blob headBlob = getBlob(headFiles.get(key));
                            Utils.writeContents(cwdFile,conflictString(headBlob, null));
                            Blob addBlob = new Blob(cwdFile);
                            File addFile = new File(Blob.BLOB_FOLDER, findBlobHash(addBlob));
                            Utils.writeObject(addFile, addBlob);
                            stageFile(findBlobHash(addBlob), key);
                        } else {
                            stageRemove(headFiles.get(key), key);
                        }

                    }

                }
                else if (givenFiles.containsKey(key)) {
                    Boolean givenModified = isModified(splitFiles.get(key), givenFiles.get(key));
                    if (givenModified) {
                        Blob givenBlob = getBlob(givenFiles.get(key));
                        Utils.writeContents(cwdFile,conflictString(null, givenBlob));
                        Blob addBlob = new Blob(cwdFile);
                        File addFile = new File(Blob.BLOB_FOLDER, findBlobHash(addBlob));
                        Utils.writeObject(addFile, addBlob);
                        stageFile(findBlobHash(addBlob), key);
                    }
                }
            }
        });
        String message = "Merged " + args[1] + " into "+Utils.readContentsAsString(headFile)+".";
        commit(new String[] {"commit", message});
        Utils.writeContents(givenBranch, getHead());
        if (_conflictUsed) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /**
     * Change the head hash
     * @param head
     */
    public static void moveHead(Commit head) {
        File headFile = new File(GITLET_DIR,"HEAD");
        File branch =  new File(BRANCHES_DIR, Utils.readContentsAsString(headFile));
        Utils.writeContents(branch, findCommitHash(head));
    }

    public static String getHead() {
        File head = new File(GITLET_DIR, "HEAD");
        File branch = new File(BRANCHES_DIR, Utils.readContentsAsString(head));
        String UID = Utils.readContentsAsString(branch);
        return UID;
    }

    public static Commit getHeadCommit() {
        File headFile = new File(Commit.COMMITS_FOLDER, getHead());
        return Utils.readObject(headFile, Commit.class);
    }
    public static Commit getCommit(String UID) {
        File UIDFile = new File(Commit.COMMITS_FOLDER, UID);
        List<String> commits = Utils.plainFilenamesIn(Commit.COMMITS_FOLDER);
        if (commits.contains(UID)) {
            return Utils.readObject(UIDFile, Commit.class);
        }
        else {
            return null;
        }
    }
    public static Blob getBlob(String UID) {
        File UIDFile = new File(Blob.BLOB_FOLDER, UID);
        List <String> blobs = Utils.plainFilenamesIn(Blob.BLOB_FOLDER);
        if (blobs != null) {
            if (blobs.contains(UID)) {
                return Utils.readObject(UIDFile, Blob.class);
            } else {
                return null;
            }
        }
        return null;
    }

    public static void stageFile(String UID, String file_name) {
        if(!StagingArea.exists()) {
            StagingArea.mkdir();
        }
        File stage = new File(StagingArea, file_name) ;
        if (stage.exists()) {
            stage.delete();
        }
        try{
            stage.createNewFile();
        } catch (IOException exp){

        }
        Utils.writeContents(stage, UID);
    }
    public static void stageOverwrite(File stage,String file_name) {
        List<String> stageStatus = Utils.plainFilenamesIn(stage);
        if (stageStatus!=null) {
            if (stageStatus.contains(file_name)) {
                //overwrite
                File oldState = new File(stage, file_name);
                oldState.delete();
            }
        }
    }
    public static void stageRemove(String UID, String file_name) {
        if(!filesForRemove.exists()) {
            filesForRemove.mkdir();
        }
        File remove = new File(filesForRemove, file_name);
        try{
            remove.createNewFile();
        } catch (IOException exp){

        }
        Utils.writeContents(remove, UID);
    }

    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            throw new RuntimeException(
                    String.format("Invalid number of arguments for: %s.", cmd));
        }
    }
    public static String findCommitHash(Commit commit) {
        if (commit.getParent()==null) {
            return Utils.sha1(commit.getMessage(), commit.getBlobs().toString(), commit.getTimeStamp().toString());
        }
        return Utils.sha1(commit.getMessage(),commit.getParent(), commit.getBlobs().toString(), commit.getTimeStamp().toString());
    }
    public static  String findBlobHash(Blob blob) {

        return Utils.sha1(blob.getContents());
    }
    public static ArrayList<String> getUntrackedFiles() {
        //not tracked or staged
        List <String> presentFiles  = Utils.plainFilenamesIn(CWD);
        List <String> stagedFiles = Utils.plainFilenamesIn(StagingArea);
        ArrayList <String> result = new ArrayList<>();
        if (presentFiles != null) {
            for (String s: presentFiles) {
                int check = 1;
                if (getHeadCommit().getBlobs().containsKey(s)) {
                    check *= 0;
                }
                if (stagedFiles != null) {
                    if (stagedFiles.contains(s)) {
                        check *= 0;
                    }
                }
                if (check == 1) {
                    result.add(s);
                }
            }
        }

        return result;
    }

    public static ArrayList<String> getAndPrintMNSCFiles () {
        ArrayList<String> result = new ArrayList<>();

        getHeadCommit().getBlobs().entrySet().forEach(entry -> {
            File thisFile = new File(CWD, entry.getKey());
            if(thisFile.exists()) {
                if(!Utils.sha1(Utils.readContentsAsString(thisFile)).equals(entry.getValue())) {
                    File stageFile = new File(StagingArea, entry.getKey());
                    if (stageFile.exists()) {
                        if (!Utils.readContentsAsString(stageFile).equals(Utils.sha1(Utils.readContentsAsString(thisFile)))) {
                            result.add(entry.getKey());
                            System.out.println(entry.getKey() + "(modified)");
                        }
                    } else {
                        result.add(entry.getKey());
                        System.out.println(entry.getKey()  + " (modified)");
                    }
                }
            } else {
                File removeFile = new File(filesForRemove, entry.getKey());
                if (!removeFile.exists()) {
                    result.add(entry.getKey());
                    System.out.println(entry.getKey()  + " (deleted)");
                }
            }
        });

        List <String> stagedFiles = Utils.plainFilenamesIn(StagingArea);
        if(stagedFiles!=null) {
            for (String s: stagedFiles) {
                File thisStagedFile = new File(StagingArea, s);
                File thisCWDFile = new File(CWD, s);
                if (thisCWDFile.exists()) {
                    if (!Utils.sha1(Utils.readContentsAsString(thisCWDFile)).equals(Utils.readContentsAsString(thisStagedFile))) {
                        if(!result.contains(s)) {
                            result.add(s);
                            System.out.println(s  + " (modified)");
                        }
                    }
                } else {
                    if(!result.contains(s)) {
                        result.add(s);
                        System.out.println(s  + " (deleted)");
                    }
                }
            }
        }
        return result;
    }
    public static String getSplitPoint(String given) {
        ArrayList<String> currentBranchHistory = new ArrayList<>();
        ArrayList<String> givenBranchHistory = new ArrayList<>();
        Commit start = getHeadCommit();
        while(true) {
            currentBranchHistory.add(findCommitHash(start));
            if (start.getParent() == null) {
                break;
            }
            start = getCommit(start.getParent());
        }
        File headFile = new File(GITLET_DIR, "HEAD");
        String currentBranch = Utils.readContentsAsString(headFile);
        headFile.delete();
        Utils.writeContents(headFile, given);
        Commit begin = getHeadCommit();
        while(true) {
            givenBranchHistory.add(findCommitHash(begin));
            if (begin.getParent() == null) {
                break;
            }
            begin = getCommit(begin.getParent());
        }
        if (currentBranchHistory.size() > 0 && givenBranchHistory.size() > 0) {
            for (int x = 0; x < currentBranchHistory.size(); x++ ) {
                if (givenBranchHistory.contains(currentBranchHistory.get(x))) {
                    headFile.delete();
                    Utils.writeContents(headFile, currentBranch);
                    return currentBranchHistory.get(x);
                }
            }
        }
        headFile.delete();
        Utils.writeContents(headFile, currentBranch);
        return null;
    }
    public static Boolean isModified(String compared, String compareTo) {
        Blob comparedBlob = getBlob(compared);
        Blob compareToBlob = getBlob(compareTo);
        if (comparedBlob != null) {
            if (compareToBlob != null) {
                if (compareToBlob.getContents().equals(comparedBlob.getContents())) {
                    return false;
                }
                return true;
            }
            return true;
        } else {
            if (compareToBlob != null) {
                return true;
            }
            return false;
        }
    }
    public static String conflictString(Blob head, Blob given) {
        _conflictUsed = true;
        if (head == null && given != null) {
            String result = "<<<<<<< HEAD\n" + "\n=======\n" + given.getContents() + "\n>>>>>>>";
            return result;
        }
        if (head != null && given == null) {
            String result = "<<<<<<< HEAD\n" + head.getContents() + "\n=======\n" + "\n>>>>>>>";
            return result;
        }
        if (head == null && given == null) {
            String result = "<<<<<<< HEAD\n" + "\n=======\n" + "\n>>>>>>>";
            return result;
        }
        String result = "<<<<<<< HEAD\n" + head.getContents() + "\n=======\n" + given.getContents() + "\n>>>>>>>";
        return result;


    }
    public static void checkInit() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    private static Boolean _conflictUsed;
}
