package nia.BlessedLavoneCodeWithEpoll_andChunkWriteHandler.filesenderdir;

public class FileTransferObject{

    private String theDestFilePath;
    private long theCurrentOffSet;
    private long theCurrentFragmentSize;
    private int theFileId;
    private String theSrcFilePath;

    public FileTransferObject(String aSrcFilePath, String aDestFilePath, long offSet, long currentFragmentSize, int aFileId){
        this.theSrcFilePath = aSrcFilePath;
        this.theDestFilePath = aDestFilePath;
        this.theCurrentOffSet = offSet;
        this.theCurrentFragmentSize = currentFragmentSize;
        this.theFileId = aFileId;
    }


    public String getTheSrcFilePath() {
        return theSrcFilePath;
    }

    public void setTheSrcFilePath(String theSrcFilePath) {
        this.theSrcFilePath = theSrcFilePath;
    }

    public String getTheDestFilePath() {
        return theDestFilePath;
    }

    public void setTheDestFilePath(String theDestFilePath) {
        this.theDestFilePath = theDestFilePath;
    }

    public long getTheCurrentOffSet() {
        return theCurrentOffSet;
    }

    public void setTheCurrentOffSet(long theCurrentOffSet) {
        this.theCurrentOffSet = theCurrentOffSet;
    }

    public long getTheCurrentFragmentSize() {
        return theCurrentFragmentSize;
    }

    public void setTheCurrentFragmentSize(long theCurrentFragmentSize) {
        this.theCurrentFragmentSize = theCurrentFragmentSize;
    }

    public int getTheFileId() {
        return theFileId;
    }

    public void setTheFileId(int theFileId) {
        this.theFileId = theFileId;
    }

}