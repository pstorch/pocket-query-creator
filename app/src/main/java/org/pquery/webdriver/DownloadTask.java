package org.pquery.webdriver;

import android.content.Context;

import org.pquery.R;
import org.pquery.util.HTTPStatusCodeException;
import org.pquery.util.IOUtils;
import org.pquery.util.IOUtils.FileDetails;
import org.pquery.util.IOUtils.Listener;
import org.pquery.util.Logger;
import org.pquery.util.Prefs;
import org.pquery.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Download file over HTTP
 */
public class DownloadTask extends RetriableTask<File> {

    private Context cxt;
    private String url;
    private File dir;
    private String filename;

    public DownloadTask(int numberOfRetries, int fromPercent, int toPercent, ProgressListener progressListener, CancelledListener cancelledListener, Context cxt, String url, File dir, String filename) {
        super(numberOfRetries, fromPercent, toPercent, progressListener, cancelledListener, cxt.getResources());
        this.cxt = cxt;
        this.url = url;
        this.dir = dir;
        this.filename = filename;
    }

    @Override
    protected File task() throws FailureException, FailurePermanentException, InterruptedException {

        FileDetails pq;

        progressReport(0, res.getString(R.string.downloading), res.getString(R.string.requesting));

        // Get the pocket query creation page
        // and read the response. Need to detect if logged in or no

        try {
            pq = IOUtils.httpGetBytes(cxt, url, cancelledListener, new Listener() {

                @Override
                public void update(int bytesReadSoFar, int expectedLength, int percent0to100) {
                    progressReport(
                            percent0to100,
                            res.getString(R.string.downloading),
                            Util.humanDownloadCounter(bytesReadSoFar, expectedLength));
                }
            });

        } catch (HTTPStatusCodeException e) {
            // When DownloadablePQ not run, we get back 302 redirect to <a href="/pocket/">
            if (e.code == HttpURLConnection.HTTP_MOVED_TEMP && e.body.indexOf("<a href=\"/pocket/\">") != -1)
                throw new FailureException(res.getString(R.string.download_not_ready));

            // Treat any other status code as error
            throw new FailureException(res.getString(R.string.download_failed), e);

        } catch (IOException e) {
            throw new FailureException(res.getString(R.string.download_failed), e);
        }

        // Write to output file

        File output = null;

        try {
            Logger.d("Going to write to file");

            // Try to get a unique output file in the given directory with given name
            // If there is a clash, it adds (1) onto the end etc.

            // HACK!
            if (Prefs.getCgeoCompatability(cxt))
                output = Util.getUniqueFile(dir.toString(), pq.filename);
            else
                output = Util.getUniqueFile(dir.toString(), filename);

            FileOutputStream fout = new FileOutputStream(output);

            fout.write(pq.contents);
            fout.close();
            Logger.d("Written to file ok");

        } catch (IOException e) {
            throw new FailurePermanentException(res.getString(R.string.unable_to_write_file));
        }

        return output;
    }

}



