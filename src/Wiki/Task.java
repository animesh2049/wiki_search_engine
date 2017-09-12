package Wiki;

public class Task {
    private String fileName;
    private long fileSize;
    private boolean jobType;
    private MergerThread whoSlept;
    private String specialInfo;
    private Integer tid;

    Task(String fileName, long fileSize, boolean jobType){
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.jobType = jobType;
    }

    Task(String fileName, long fileSize, boolean jobType, String info){
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.jobType = jobType;
        this.specialInfo = info;
    }

    Task(MergerThread producer, boolean jobType) {
        this.whoSlept = producer;
        this.jobType = jobType;
        this.tid = producer.getTid();
    }

    public boolean getJobType() {
        return this.jobType;
    }

    public String getSpecialInfo() {
        return this.specialInfo;
    }

    public String getFileName() {
        return this.fileName;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public MergerThread getWhoSlept() {
        return this.whoSlept;
    }

    public Integer getTid() {
        return this.tid;
    }
}
