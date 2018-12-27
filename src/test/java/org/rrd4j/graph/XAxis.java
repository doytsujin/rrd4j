package org.rrd4j.graph;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;
import org.rrd4j.core.Util;

public abstract class XAxis<T extends Axis> extends DummyGraph {

    static private RrdBackendFactory previousBackend;

    @BeforeClass
    public static void setBackendBefore() {
        previousBackend = RrdBackendFactory.getDefaultFactory();
        RrdBackendFactory.setActiveFactories(new RrdRandomAccessFileBackendFactory());
    }

    @AfterClass
    public static void setBackendAfter() {
        RrdBackendFactory.setActiveFactories(previousBackend);
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Rule
    public TestName name = new TestName();


    String jrbFileName;
    final long startTime = 1;
    private T valueAxis;

    @Before 
    public void setup() throws IOException {
        jrbFileName = testFolder.newFile("test-value-axis.rrd").getCanonicalPath();
    }

    void createGaugeRrd(int rowCount) throws IOException {
        RrdDef def = new RrdDef(jrbFileName);
        def.setStartTime(startTime);
        def.setStep(60);
        def.addDatasource("testvalue", DsType.GAUGE, 120, Double.NaN, Double.NaN);
        def.addArchive("RRA:AVERAGE:0.5:1:" + rowCount);

        //Create the empty rrd.  Other code may open and append data
        try (RrdDb rrd = new RrdDb(def)) {
        }

    }

    //Cannot be called until the RRD has been populated; wait
    void prepareGraph() throws IOException {

        graphDef = new RrdGraphDef();
        graphDef.datasource("testvalue", jrbFileName, "testvalue", ConsolFun.AVERAGE);
        graphDef.area("testvalue", Util.parseColor("#FF0000"), "TestValue");
        graphDef.setStartTime(startTime);
        graphDef.setEndTime(startTime + (60*60*24));
        graphDef.setLocale(Locale.US);
//        graphDef.setFilename("/tmp/" + name.getMethodName() + ".png");
//        graphDef.setImageFormat("png");
//        graphDef.setColor(ElementsNames.grid, Color.GREEN);
//        graphDef.setTickStroke(new BasicStroke(5.0f));

        setupGraphDef();

        //There's only a couple of methods of ImageWorker that we actually care about in this test.
        // More to the point, we want the rest to work as normal (like getFontHeight, getFontAscent etc)
        imageWorker = createMockBuilder(ImageWorker.class)
                .addMockedMethod("drawLine")
                .addMockedMethod("drawString")
                .createMock(); //Order is important!

        buildGraph();

        valueAxis = makeAxis();
        
        //RrdGraph graph = new RrdGraph(graphDef);

    }

    void run() {
        replay(imageWorker);

        Assert.assertTrue(valueAxis.draw());

        //Validate the calls to the imageWorker
        verify(imageWorker);
    }

    abstract void setupGraphDef();

    abstract T makeAxis();

}
