package xyz.kumaraswamy.nativeinject;

import android.os.Build;
import android.util.Log;
import com.google.appinventor.components.runtime.Form;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NativeInject {

  private static final String TAG = "NativeInject";

  private final Form form;

  private final String classPackageName;

  public NativeInject(Class<?> extensionComponent) {
    classPackageName = extensionComponent.getPackage().getName();
    form = Form.getActiveForm();
  }

  public void beginUnpack(String name) throws IOException {
    // first before opening the 7zip file, we have to copy it to the
    // files directory, then open it
    InputStream input = form.getAssets().open(classPackageName + '/' + name);
    File sevenZFile = copyStreamFilesDirectory(name, input);
    input.close();

    ZipInputStream zFile = null;
    try {
      zFile = new ZipInputStream(new FileInputStream(sevenZFile));

      ZipEntry entry;
      while ((entry = zFile.getNextEntry()) != null) {
        // iterate over all the enteries in the 7zip file and copy
        // them over to the application's files directory
        copyStreamFilesDirectory(entry.getName(), zFile);
      }
    } finally {
      if (zFile != null) {
        zFile.close();
      }
    }
  }

  public boolean loadAll() throws IOException {
    String[] archsSupported = Build.SUPPORTED_ABIS;

    String[] extAssets = form.getFilesDir().list();

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
      // the native library is already loaded, this one is another
      // arch version of the same library
      if (loadedLibs.contains(libName))
        continue;
      for (String arch: archsSupported) {
        // try to find the matching architecture of the
        // native library
        if (!arch.equals(archName))
          continue;
        Log.d(TAG, "Loading Native " + libName + " arch = " + arch);

        // the files were already extracted to the files directory
        String filePath = new File(form.getFilesDir(), asset).getAbsolutePath();
        System.load(filePath);

        loadedLibs.add(libName);
        continue load;
      }
      Log.d(TAG, "Failed to find supporting arch for " + asset);
      return false;
    }
    Log.d(TAG, "Native Loading Successful");
    // TODO:
    //  we gotta optimize the code later to avoid
    //  multiple repetitions
    return true;
  }


  private File copyStreamFilesDirectory(String fileName, InputStream input) throws IOException {
    File file = new File(form.getFilesDir(), fileName);
    FileOutputStream output = new FileOutputStream(file);

    copy(input, output);

    output.close();

    return file;
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
