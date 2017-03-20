/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.amazon.alexa.avs;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.MultichannelToMono;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.resample.RateTransposer;

import java.util.concurrent.ThreadLocalRandom;
/**
 *
 * @author debdo
 */
public class PitchShift {
    private AudioDispatcher dispatcher;
    private WaveformSimilarityBasedOverlapAdd wsola;
    private GainProcessor gain;
    private AudioPlayer audioPlayer;
    private RateTransposer rateTransposer;
    private double currentFactor;// pitch shift factor
    private double sampleRate;
    private boolean loop;

    public PitchShift() {
        this.loop = false;
        int cents = ThreadLocalRandom.current().nextInt(-800, 800 + 1);;
        this.currentFactor = 1 / Math.pow(Math.E, cents*Math.log(2)/1200/Math.log(Math.E)); ;
        
    }
    
    public void startFile(final File inputFile){
	if (dispatcher != null){
            dispatcher.stop();
	}
	AudioFormat format;
	try {
            if (inputFile != null){
		format = AudioSystem.getAudioFileFormat(inputFile).getFormat();
            } else {
		format = new AudioFormat(44100, 16, 1, true,true);
            }
            
            rateTransposer = new RateTransposer(currentFactor);
            gain = new GainProcessor(1.0);
            audioPlayer = new AudioPlayer(format);
            sampleRate = format.getSampleRate();
			
            wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.musicDefaults(currentFactor, sampleRate));
		
            if (format.getChannels() != 1){
                dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize() * format.getChannels(),wsola.getOverlap() * format.getChannels());
                dispatcher.addAudioProcessor(new MultichannelToMono(format.getChannels(),true));
            } else{
                dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
            }
                
            wsola.setDispatcher(dispatcher);
            dispatcher.addAudioProcessor(wsola);
            dispatcher.addAudioProcessor(rateTransposer);
            dispatcher.addAudioProcessor(gain);
            dispatcher.addAudioProcessor(audioPlayer);
            dispatcher.addAudioProcessor(new AudioProcessor() {
				
		@Override
		public void processingFinished() {
                    if (loop){
			dispatcher = null;
			startFile(inputFile);
                    }
                }
				
		@Override
                    public boolean process(AudioEvent audioEvent) {
			return true;
                    }
		});

            Thread t = new Thread(dispatcher);
            t.start();
            } catch (UnsupportedAudioFileException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
            } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
            } catch (LineUnavailableException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
            }	
	}
}
