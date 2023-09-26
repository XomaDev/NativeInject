package xyz.kumaraswamy.nativeinject;

import android.os.Build;
import android.util.Log;
import com.google.appinventor.components.runtime.Form;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class NativeInject {

  private static final String TAG = "NativeInject";

  private final String classPackageName;

  public NativeInject(Class<?> extensionComponent) {
    classPackageName = extensionComponent.getPackage().getName();
  }

  public boolean loadAll() throws IOException {
    String[] archsSupported = Build.SUPPORTED_ABIS;

    Form form = Form.getActiveForm();
    String[] extAssets = form.getAssets().list(classPackageName);

    List<String> loadedLibs = new ArrayList<>();

    load:
    for (String asset : extAssets) {
      // search for native libraries
      if (!asset.endsWith(".so")) continue;
      // copy it from the assets directory to the
      // files directory
      String[] nameSplit = asset.split("!");
      if (nameSplit.length != 2) {
        Log.d(TAG, "Skipping Unknown Format " + asset);
        continue;
      }
      String archName = nameSplit[0], libName = nameSplit[1];
      if (loadedLibs.contains(libName))
        // the native library is already loaded, this one is another
        // arch version of the same libary
        continue;
      for (String arch: archsSupported) {
        // try to find the matching architecture of the
        // native library
        if (!arch.equals(archName)) continue;
        Log.d(TAG, "Loading Native " + libName + " arch = " + arch);

        String filePath = copyFilesDirectory(libName, asset);
        System.load(filePath);

        loadedLibs.add(libName);
        continue load;
      }
      Log.d(TAG, "Failed to find supporting arch for " + asset);
      return false;
    }
    return true;
    // TODO:
    //  next time loadAll() is called, we first must check if
    //  it's already loaded in order to reduce execution time
  }

  private String copyFilesDirectory(String libName, String asset) throws IOException {
    Form form = Form.getActiveForm();

    File nativeFile = new File(form.getFilesDir(), libName);

    InputStream input = form.getAssets().open(classPackageName + '/' + asset);
    OutputStream output = new FileOutputStream(nativeFile);

    copy(input, output);

    input.close();
    output.close();

    return nativeFile.getAbsolutePath();
  }

  private long copy(InputStream source, OutputStream sink)
    throws IOException {
    long nread = 0L;
    byte[] buf = new byte[8192];
    int n;
    while ((n = source.read(buf)) > 0) {
      sink.write(buf, 0, n);
      nread += n;
    }
    return nread;
  }
}
