// Optical signal acquisition: no initial frame is assumed to be black.
export class LuminanceGate {
 constructor({minContrast=8,onEdge=()=>{}}={}){this.minContrast=minContrast;this.onEdge=onEdge;this.reset();}
 reset(){this.history=[];this.lit=false;this.candidate=null;this.previousTime=undefined;this.edgeTime=0;this.transitions=0;this.low=0;this.high=0;this.contrast=0;this.lowRgb=[0,0,0];}
 feed(time,rgb){
  const level=Math.max(...rgb),sample={time,level,rgb};this.history.push(sample);while(this.history.length>1&&this.history[0].time<time-7000)this.history.shift();
  const sorted=[...this.history].sort((a,b)=>a.level-b.level),n=sorted.length;
  const lo=sorted[Math.floor((n-1)*.01)],hi=sorted[Math.ceil((n-1)*.99)];this.low=lo.level;this.high=hi.level;this.lowRgb=lo.rgb;this.contrast=this.high-this.low;
  if(this.contrast>=this.minContrast){
   const midpoint=(this.low+this.high)/2,hysteresis=this.contrast*.12;
   const next=this.lit?level>midpoint-hysteresis:level>=midpoint+hysteresis;
   if(next!==this.lit){
    if(!this.candidate||this.candidate.lit!==next)this.candidate={lit:next,time:(time+(this.previousTime??time))/2,count:1};else this.candidate.count++;
    if(this.candidate.count>=((time-(this.previousTime??time))>50?1:2)&&time-this.candidate.time>=25){this.lit=next;this.edgeTime=this.candidate.time;this.candidate=null;this.transitions++;this.onEdge({time:this.edgeTime,lit:next,contrast:this.contrast});}
   }else this.candidate=null;
  }else this.candidate=null;
  this.previousTime=time;return {lit:this.lit,edgeTime:this.edgeTime,level,low:this.low,high:this.high,contrast:this.contrast,transitions:this.transitions};
 }
}

// Finds temporal changes, rather than assuming the brightest object is the sender.
// A one-pixel emitter is preserved: no averaging across the whole region.
export class TemporalSpot {
 constructor(){this.reset();}
 reset(){this.previous=null;this.activity=null;this.anchor=null;this.weights=null;}
 sample(data,width,height,{locked=false}={}){
  const count=width*height;if(!this.previous||this.previous.length!==count){this.previous=new Float32Array(count);this.activity=new Float32Array(count);this.weights=new Float32Array(count);for(let i=0;i<count;i++){this.previous[i]=Math.max(data[i*4],data[i*4+1],data[i*4+2]);this.weights[i]=1/(1+Math.hypot(i%width-width/2,Math.floor(i/width)-height/2)/Math.max(width,height));}this.anchor=Math.floor(height/2)*width+Math.floor(width/2);}
  let best=this.anchor,bestScore=0,peak=0;
  for(let i=0;i<count;i++){const offset=i*4,v=Math.max(data[offset],data[offset+1],data[offset+2]);this.activity[i]=this.activity[i]*.94+Math.abs(v-this.previous[i])*.18;this.previous[i]=v;const score=this.activity[i]*this.weights[i];if(score>bestScore){bestScore=score;best=i;}peak=Math.max(peak,this.activity[i]);}
  // Read the changing pixel itself. Selecting the brightest neighbour can
  // substitute a streetlamp or a saturated reflection for the optical source.
  if(!locked&&bestScore>2.5)this.anchor=best;
  const chosen=this.anchor,ax=chosen%width,ay=Math.floor(chosen/width);
  return {rgb:[data[chosen*4],data[chosen*4+1],data[chosen*4+2]],x:ax,y:ay,score:bestScore,activity:peak};
 }
}
