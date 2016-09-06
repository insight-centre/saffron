/*
 *
 * GateProcessorProvider.java, provides topic extraction as a Java program/API
 * Copyright (C) 2008  Alexander Schutz
 * National University of Ireland, Galway
 * Digital Enterprise Research Institute
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package org.insightcentre.nlp.saffron.topicextraction.topicextractor.gate;

import gate.util.GateException;

import java.util.concurrent.ArrayBlockingQueue;


/**
 * Provides a number of GateProcessors in a pool -- implemented as an
 * ArrayBlockingQueue of GateProcessors.
 * <p>
 * 
 * @author alesch
 * 
 */
class GateProcessorPool extends ArrayBlockingQueue<GateProcessor> {
	private static final long serialVersionUID = -5540696752870804820L;

	/**
	 * increase this in case multiple simultaneous requests are to be supported
	 */
	

	private static GateProcessorPool _instance = null;
	
	/**
	 * 
	 */
	public static final GateProcessorPool getInstance() throws GateException {
		if (_instance == null) {
			Integer numPipelines;
			String numPipelinesStr = System.getProperty("saffron.numPipelines");
			
			if (numPipelinesStr != null) {
				numPipelines = Integer.parseInt(numPipelinesStr);
			} else {
				numPipelines = 2;
			}
//			logger.info(String.format("Number of GATE pipelines: %d", numPipelines));
			_instance = new GateProcessorPool(numPipelines);
		}
		return _instance;
	}
	
	private GateProcessorPool(Integer numPipelines) throws GateException {
		super(numPipelines);
		init();
	}

	/**
	 * Initializes a number of GateProcessors as specified by the capacity
	 * parameter of the constructor
	 * 
	 * @throws GateException
	 */
	private void init() throws GateException {
		while (this.remainingCapacity() > 0) {
			if (this.offer(new GateProcessor())) {
//				logger.log(Level.INFO, "added " + GateProcessor.class.getName()
//						+ " to ProcessorQueue");
			}
		}
	}
}
