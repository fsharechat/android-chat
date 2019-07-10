package cn.wildfirechat.proto.store;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

abstract class SqliteDatabaseStore extends DataStoreAdapter{

    Log logger = LoggerFactory.getLogger(SqliteDatabaseStore.class);
    protected SQLiteDatabase database;
    protected ChatStoreHelper dbHelper;
    private static final String DELETE_TABLE_MESSAGES = "delete from "+ChatStoreHelper.TABLE_MESSAGES;
    private static final String DELETE_TABLE_CONVERSATIONS = "delete from "+ChatStoreHelper.TABLE_CONVERSATIONS;


    protected SqliteDatabaseStore(Context context){
        dbHelper = ChatStoreHelper.getInstance(context);
        open();
    }

    private void open() {
        if (!isDatabaseOpen()) {
            try {
                database = dbHelper.getWritableDatabase();
                database.enableWriteAheadLogging();
            } catch (Exception e){
                logger.e(" open database error "+e.getMessage());
            }
        }
    }

    public boolean isDatabaseOpen() {
        return database != null && database.isOpen();
    }

    protected byte[] serialize(Object serialObj) {
        try {
            ByteArrayOutputStream mem_out = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(mem_out);
            out.writeObject(serialObj);
            out.close();
            mem_out.close();
            return mem_out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Object deserializer(byte[] bytes) {
        try {
            ByteArrayInputStream mem_in = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(mem_in);
            Object resultObj = in.readObject();
            in.close();
            mem_in.close();
            return resultObj;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void stop() {
        if(isDatabaseOpen()){
            database.execSQL(DELETE_TABLE_CONVERSATIONS);
            database.execSQL(DELETE_TABLE_MESSAGES);
        }
    }
}
