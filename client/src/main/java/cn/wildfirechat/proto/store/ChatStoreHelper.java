package cn.wildfirechat.proto.store;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;

public class ChatStoreHelper extends SQLiteOpenHelper {
    Log logger = LoggerFactory.getLogger(ChatStoreHelper.class);
    private static final String TAG                 = ChatStoreHelper.class.getName();
    public static final String DATABASE_NAME       = "chat.db";
    public static final String TABLE_MESSAGES         = "messages";
    private static final String queryDropTable =
            "DROP TABLE IF EXISTS '" + TABLE_MESSAGES + "'";
    private static final String queryCreateMessagesTable = "CREATE TABLE IF NOT EXISTS 'messages' " +
            "(id INTEGER PRIMARY KEY," +
            " target TEXT," +
            " message_id DOUBLE DEFAULT 0," +
            " message_uid DOUBLE DEFAULT 0," +
            " message_data BLOB, " +
            " date_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    private static final int DATABASE_VERSION       = 1;

    private static ChatStoreHelper sInstance;

    public static ChatStoreHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ChatStoreHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private ChatStoreHelper(Context context){
        this(context,DATABASE_NAME);
    }

    private ChatStoreHelper(Context context,String databaseName){
        super(context,databaseName,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(queryCreateMessagesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        logger.i(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ". Destroying old data now..");
        db.execSQL(queryDropTable);
        onCreate(db);
    }
}
