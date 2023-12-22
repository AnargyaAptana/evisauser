package ai.seventhsense.evisauser.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.util.Objects;

/**
 * A key value store for storing the gallery bucket id
 */
public class KeyValueStore {
    private SharedPreferences sharedPreferences;
    private Context context;
    private static final String KEY_GALLERY_BUCKET_ID = "KEY_GALLERY_BUCKET_ID";
    private static final String KEY_PORTRAIT_MODE = "KEY_PORTRAIT_MODE";
    private static KeyValueStore instance;

    MasterKey getMasterKey() {
        try {
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder("_androidx_security_master_key_", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build();

            return new MasterKey.Builder(context).setKeyGenParameterSpec(spec).build();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error on getting master key", e);
        }
        return null;
    }


    private SharedPreferences getEncryptedSharedPreferences() {
        try {
            MasterKey masterKey = getMasterKey();
            return EncryptedSharedPreferences.create(Objects.requireNonNull(context), "ai.seventhsense.sdk7", masterKey, // calling the method above for creating MasterKey
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error on getting encrypted shared preferences", e);
        }
        return null;
    }


    private KeyValueStore(Context context) {
        this.context = context;
        sharedPreferences = getEncryptedSharedPreferences();
    }


    /**
     * Gets the instance of the key value store
     *
     * @param context the context
     * @return the instance of the key value store
     */
    public static synchronized KeyValueStore getInstance(Context context) {
        if (instance != null) {
            return instance;
        }
        instance = new KeyValueStore(context);
        return instance;
    }

    /**
     * Gets the gallery bucket id
     *
     * @return the gallery bucket id
     */
    public String getGalleryBucketId() {
        String bucketId = sharedPreferences.getString(KEY_GALLERY_BUCKET_ID, null);
        return bucketId;
    }

    /**
     * Gets the screen portrait mode
     *
     * @return the screen mode is portrait or not
     */
    public Boolean getPortraitMode() {
        return  sharedPreferences.getBoolean(KEY_PORTRAIT_MODE, true);

    }

    /**
     * Sets the screen mode
     *
     * @param screenMode the screen mode
     */
    public void setScreenMode(boolean screenMode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_PORTRAIT_MODE, screenMode);
        editor.apply();
        editor.commit();
    }


    /**
     * Sets the gallery bucket id
     *
     * @param bucketId the gallery bucket id
     */
    public void setGalleryBucketId(String bucketId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_GALLERY_BUCKET_ID, bucketId);
        editor.apply();
        editor.commit();
    }

    /**
     * Resets the value for the potrait/landscape mode
     */
    public void resetPortraitMode() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_PORTRAIT_MODE);
        editor.apply();
        editor.commit();
    }


}