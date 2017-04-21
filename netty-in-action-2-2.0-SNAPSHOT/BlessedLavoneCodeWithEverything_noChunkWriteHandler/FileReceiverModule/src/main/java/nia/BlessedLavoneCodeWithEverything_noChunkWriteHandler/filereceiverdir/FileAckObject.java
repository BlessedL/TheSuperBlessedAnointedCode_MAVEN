package nia.BlessedLavoneCodeWithEverything_noChunkWriteHandler.filereceiverdir;

public class FileAckObject{
    int myDataChannelId;
    long bytesRead;
    long startTime;
    long endTime;

    public FileAckObject(int aDataChannelId, long theBytesRead, long aStartTime, long anEndTime){
        myDataChannelId = aDataChannelId;
        bytesRead = theBytesRead;
        startTime = aStartTime;
        endTime = anEndTime;
    }

    public int getDataChannelId(){
        return myDataChannelId;
    }

    public void setDataChannelId(int aDataChannelId){
        myDataChannelId = aDataChannelId;
    }

    public long getBytesRead(){
        return bytesRead;
    }

    public void setBytesRead(long theBytesRead){
        bytesRead = theBytesRead;
    }

    public long getStartTime(){
        return startTime;
    }

    public void setStartTime(long aStartTime){
        startTime = aStartTime;
    }

    public long getEndTime(){
        return endTime;
    }

    public void setEndTime(long anEndTime){
        endTime = anEndTime;
    }
}