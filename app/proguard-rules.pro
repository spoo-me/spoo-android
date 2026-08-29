# Room builds its generated *_Impl classes reflectively, so R8 full mode
# drops the no-arg constructor and WorkManager dies initialising its
# database before any of our code runs.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# WorkManager instantiates workers by name, so the constructor signature
# has to survive too.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
