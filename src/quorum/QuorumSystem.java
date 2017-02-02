/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package quorum;

/**
 *
 * @author eduardo
 */
public interface QuorumSystem {
    
    public void write(Object value);
    public Object read();
    
}
