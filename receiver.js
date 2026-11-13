import {OpticalDecoder} from './optics.js?v=4.0.0';
import {TemporalSpot} from './signal.js?v=4.0.0';
// Live camera and saved-video frames use the exact same sampling and decoder.
export class LightReceiver{
 constructor({inbox,onState=()=>{},onSignal=()=>{},onError=()=>{}}={}){
  this.inbox=inbox;this.onState=onState;this.onSignal=onSignal;this.onError=onError;this.spot=new TemporalSpot();
  this.canvas=typeof OffscreenCanvas!=='undefined'?new OffscreenCanvas(72,72):Object.assign(document.createElement('canvas'),{width:72,height:72});this.context=this.canvas.getContext('2d',{willReadFrequently:true});
  this.decoder=new OpticalDecoder({threshold:8,onFrame:f=>{try{inbox.accept(f);}catch(e){onError(e.message);}},onState,onSignal,onError,onBeacon:()=>onState('beacon')});this.lastTime=-Infinity;this.frames=0;
 }
 reset(){this.decoder.reset();this.spot.reset();this.lastTime=-Infinity;this.frames=0;}
 feed(source,time,width,height,point){
  if(time<=this.lastTime)return;this.lastTime=time;this.frames++;
  const size=Math.min(72,width,height),x=Math.max(0,Math.min(width-size,Math.round(point.x*width-size/2))),y=Math.max(0,Math.min(height-size,Math.round(point.y*height-size/2)));
  this.context.drawImage(source,x,y,size,size,0,0,72,72);const sample=this.spot.sample(this.context.getImageData(0,0,72,72).data,72,72,{locked:this.decoder.phase!=='idle'});this.decoder.feed(time,sample.rgb);
 }
}
