package com.porfiriopartida.remotecontroller.utils.image;

import com.porfiriopartida.remotecontroller.screen.config.MouseConfig;
import com.porfiriopartida.remotecontroller.screen.config.ScreenSizeConfig;
import com.porfiriopartida.remotecontroller.screen.config.capture.ScreenCaptureConfig;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

@Component
public class RobotUtils {
    private static final Logger logger = LogManager.getLogger(RobotUtils.class);
    private static final String OUT_DIR = "D:\\Java\\remote-controller\\remote-controller\\src\\main\\resources\\out\\";
    private static final int TRANSPARENT_PIXEL = -1;

    //Do not autowire or it will cause JUnit problems because of the headless environment issue.
    private Robot robot;

    public RobotUtils(){
        if("false".equalsIgnoreCase(System.getProperty("java.awt.headless"))){
            try {
                robot = new Robot();
            } catch (AWTException e) {
                logger.error(String.format("Cannot start Robot instance %s", e.getMessage()));
            }
        }
    }

    @Autowired
    private ScreenCaptureConfig screenCaptureConfig;

    public ScreenCaptureConfig getScreenCaptureConfig() {
        return screenCaptureConfig;
    }

    public void setScreenCaptureConfig(ScreenCaptureConfig screenCaptureConfig) {
        this.screenCaptureConfig = screenCaptureConfig;
    }

    public ScreenSizeConfig getSizeConfig() {
        return sizeConfig;
    }

    public void setSizeConfig(ScreenSizeConfig sizeConfig) {
        this.sizeConfig = sizeConfig;
    }

    public MouseConfig getMouseConfig() {
        return mouseConfig;
    }

    public void setMouseConfig(MouseConfig mouseConfig) {
        this.mouseConfig = mouseConfig;
    }

    @Autowired
    private ScreenSizeConfig sizeConfig;

    @Autowired
    private MouseConfig mouseConfig;

    public BufferedImage getImage(int x, int y, int w, int h) {
        return this.getImage(x, y, w, h, false);
    }
    public BufferedImage getImage(int x, int y, int w, int h, boolean centered) {

        Rectangle rectangle;
        int realW =  w > 0 ? w: screenCaptureConfig.getSmall().getWidth();
        int realH =  h > 0 ? h: screenCaptureConfig.getSmall().getHeight();

//        if(centered){
//            x -= realW/2;
//            y -= realH/2;
//        }
//        bufferedImage.getGraphics().drawString("x", w/2, h/2);
        rectangle = new Rectangle(x, y, realW, realH);


        return robot.createScreenCapture(rectangle);
    }
    public byte[] getImageAsBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
    public String getImageAsString(byte[] image){
        return Base64.encode(image);
    }

    public String getImageAsString(int x, int y, int w, int h) throws AWTException, IOException {
        return this.getImageAsString("%s", x, y, w, h);
    }
    /**
     *
     * @param imageWrapper IMG_SRC: <img src="data:image/png;base64,%s" />, this method will call String.format(imageWrapper, imageAsString).
     *                     Pass "%s" if you want the raw image byte as astring
     * @param x Coord X of the upper left corner
     * @param y Coord Y of the upper left corner
     * @param w Width of the image
     * @param h Height of the image
     * @return A base64 image string
     *
     * @throws IOException When Robot or image parsing fail.
     */
    public String getImageAsString(String imageWrapper, int x, int y, int w, int h) throws AWTException, IOException {
        BufferedImage bufferedImage = getImage(x, y, w > 0 ? w: screenCaptureConfig.getSmall().getWidth(), h>0 ? h: screenCaptureConfig.getSmall().getHeight());

        String encodedImage = getImageAsString(getImageAsBytes(bufferedImage));

        return String.format(imageWrapper, encodedImage);
    }

    public boolean triggerClick(int x, int y, int count) throws InterruptedException {
        return this.triggerClick(new Point(x, y), count);
    }

    public boolean triggerClick(Point coords, int count) throws InterruptedException {
        robot.mouseMove((int)coords.getX(), (int)coords.getY());
        Thread.sleep(200); //TODO: Config
        if(!isMouseInPosition(coords)){
            logger.warn("Mouse was not moved to the right position.");
            return false;
        }
        for(int i = 0; i<count && i < mouseConfig.getMaxClicks(); i++){
            robot.mousePress(InputEvent.BUTTON1_MASK);
            Thread.sleep(mouseConfig.getPressDelay());
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            Thread.sleep(500);
        }
        return true;
    }

    public int[][] getRgbFromImage(String filename) throws IOException {
        BufferedImage bufferedImage = getImage(filename);
        return getRgbFromImage(bufferedImage);
    }
    private HashMap<String, BufferedImage> cache = new HashMap<String, BufferedImage>();
    public BufferedImage getImage(String filename) throws IOException {
        if(cache.get(filename) != null){
            return cache.get(filename);
        }
        BufferedImage newImage;
        try{
            newImage = ImageIO.read(new File(getInputFilename(filename)));
            cache.put(filename, newImage);
        } catch (IOException e){
            logger.error(String.format("Error reading file: %s", filename));
            throw e;
        }
        return newImage;
    }
    public String getInputFilename(String filename){
        return filename;
    }
    public int[][] getRgbFromImage(BufferedImage bufferedImage) {
        int[][] data = new int[bufferedImage.getWidth()][bufferedImage.getHeight()];

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = bufferedImage.getRGB(x, y);
                if(isTransparent(rgb)){
                    data[x][y] = TRANSPARENT_PIXEL;
                } else {
                    data[x][y] = rgb;
                }
            }
        }

        return data;
    }
    public boolean isTransparent( int pixel) {
        return (pixel >> 24) == 0x00;
    }

    public boolean clickOnScreen(boolean waitUntilPresent, String ... filenames) throws AWTException, InterruptedException, IOException {
        if(!waitUntilPresent){
            for( String filename : filenames){
                boolean result = clickOnScreen(filename);
                if(result){
                    return true;
                }
            }
            return false;
        }
        long now = Calendar.getInstance().getTimeInMillis();
        while(true){
            for( String filename : filenames){
                Point coords = findOnScreen(filename);
                if(coords != null){
                    return clickOnScreen(coords);
                }
            }
            //TODO: move to configs.
            Thread.sleep(10);
            //TODO: move to configs. 60000
            if(Calendar.getInstance().getTimeInMillis() - now > 60000){
                System.err.println("Image not found after 60 seconds.");
                break;
            }
        }
        return false;
    }
    private boolean isMouseInPosition(Point coords){
        return MouseInfo.getPointerInfo().getLocation().equals(coords);
    }

    public boolean clickOnScreen(Point coords) throws IOException, AWTException, InterruptedException {
        if(coords != null){
            return triggerClick(coords, 1);
        }
        return false;
    }
    public boolean clickOnScreen(String filename) throws IOException, AWTException, InterruptedException {
        Point coords = findOnScreen(filename);
        return clickOnScreen(coords);
    }

    public Point findOnScreen(String filename) throws IOException, AWTException {
        int width = sizeConfig.getWidth();
        int height = sizeConfig.getHeight();

        BufferedImage fullScreen = getImage(0, 0, width, height);
        BufferedImage subImage = getImage(filename );

//        String uuid = UUID.randomUUID().toString();
//        saveImage(fullScreen, String.format("%s\\full.png", uuid));
//        saveImage(subImage, String.format("%s\\sub.png", uuid));

        int[][] subData = getRgbFromImage(subImage);
//        printPixels(subData, "X", "-");
        int[][] data = getRgbFromImage(fullScreen);

        Point topLeftPoint = matrixContains(data, subData);
        if(topLeftPoint == null){
            return null;
        }
        int centeredX = (int) topLeftPoint.getX() + subImage.getWidth()/2;
        int centeredY = (int) topLeftPoint.getY() + subImage.getHeight()/2;

        return new Point( centeredX, centeredY);
    }

    private void printPixels(int[][] subData, String dataPixel, String emptyPixel) {
        StringBuilder str = new StringBuilder();
        for(int i=0;i<subData.length;i++){
            for(int j=0;j<subData[0].length;j++) {
                if(subData[i][j] == TRANSPARENT_PIXEL){
                    str.append(emptyPixel);
                } else {
                    str.append(dataPixel);
                }
            }
            str.append("\n");
        }
        logger.debug(str.toString());
    }

    public Point matrixContains(int[][] fullScreenData, int[][] subImageData) {
        for (int or = 0; or <= fullScreenData.length - subImageData.length; or++) {
            outerCol:
            for (int oc = 0; oc <= fullScreenData[or].length - subImageData[0].length; oc++) {
                for (int ir = 0; ir < subImageData.length; ir++)
                    for (int ic = 0; ic < subImageData[ir].length; ic++)
                        if (fullScreenData[or + ir][oc + ic] != subImageData[ir][ic])
                        {
                            if(subImageData[ir][ic] != TRANSPARENT_PIXEL){
                                continue outerCol;
                            } else{
                                logger.debug("Matching as transparent pixel.");
                            }
                        }
                return new Point(or, oc);
            }
        }

        return null;
    }
    public void saveImage(BufferedImage bImage, String filename){
        String newFile = OUT_DIR + filename;
        try {
            File f = new File(newFile);
            if(!f.getParentFile().mkdir()){
                throw new FileNotFoundException(String.format("Couldn't directory for %s", filename));
            }
            if(!f.createNewFile()){
                throw new FileNotFoundException(String.format("Couldn't create file %s", newFile));
            }
            ImageIO.write(bImage, "jpg", f);
        } catch (IOException e) {
            logger.error("Exception occured: " + e.getMessage());
        }
        logger.debug("Images were written succesfully.");
    }

    private String getUser() throws IOException, AWTException {
        String[] files = new String[]{"fn_1.png", "fn_2.png", "eyeshield.png" };
        while(true){
            for (int i = 0; i < files.length; i++) {
                Point coords = findOnScreen(files[i]);
                if(coords != null){
                    return files[i];
                }
            }
        }
    }

    public Robot getRobot() {
        return robot;
    }

    public void setRobot(Robot robot) {
        this.robot = robot;
    }
}
