package com.denisgalajda.parserwithdatabase;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Denis Galajda on 21.3.17.
 */

class SqlXmlParser {
    private XmlPullParser mParser;

    SqlXmlParser() throws XmlPullParserException {
        XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
        mParser = pullParserFactory.newPullParser();
        mParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
    }

    ArrayList<String> parse(InputStream inputStream) throws XmlPullParserException, IOException {
        ArrayList<String> sqlQueries = null;

        mParser.setInput(inputStream, null);
        int eventType = mParser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName;

            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    sqlQueries = new ArrayList<>();
                    break;
                case XmlPullParser.START_TAG:
                    tagName = mParser.getName();
                    if (tagName.equals("query")) {
                        assert sqlQueries != null;
                        sqlQueries.add(mParser.nextText());
                    }
                    break;
                // davam to tu len pre pripad, ze by si menil format XMLka poreboval aj tento case - inac mozes zmazat
                case XmlPullParser.END_TAG:
                    break;
            }

            eventType = mParser.next();
        }

        return sqlQueries;
    }
}
