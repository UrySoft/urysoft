// Detect the four black/white finder centres; manual coordinates remain available.
export function findCorners({pixels,width:w,height:h}){
 const stride=pixels.length/w/h;
 function dark(x,y){const k=(y*w+x)*stride;return pixels[k]+pixels[k+1]+pixels[k+2]<300}
 const groups=[];
 for(let y=1;y<h-1;y+=Math.max(1,Math.floor(h/1300))){
  let runs=[],start=0,shade=dark(0,y);
  for(let x=1;x<=w;x++){const next=x<w&&dark(x,y);if(x<w&&next===shade)continue;
   runs.push({start,len:x-start,shade});if(runs.length>5)runs.shift();
   if(shade&&runs.length===5){const [a,b,c,d,e]=runs;const u=(b.len+d.len)/2;
    if(u>=1&&[a.len,b.len,d.len,e.len].every(v=>v>=.45*u&&v<=1.8*u)&&c.len>=1.9*u&&c.len<=4.5*u){
     const cx=Math.floor(c.start+c.len/2);
     let top=y,bot=y;while(top>0&&dark(cx,top-1))top--;while(bot<h-1&&dark(cx,bot+1))bot++;
     const cy=Math.round((top+bot)/2),v=(bot-top+1)/3;
     if(v>.4*u&&v<2.5*u&&cy-3*v>=0&&cy+3*v<h&&[-2,2].every(i=>!dark(cx,Math.round(cy+i*v)))&&[-3,3].every(i=>dark(cx,Math.round(cy+i*v)))){
      let g=groups.find(g=>Math.abs(g.x-cx)<2*u&&Math.abs(g.y-cy)<2*v);
      if(g)g.hits++;else if(groups.length<1000)groups.push({x:cx,y:cy,hits:1,unit:u});
     }
    }
   }
   start=x;shade=next;
  }
 }
 const targets=[[0,0],[w,0],[w,h],[0,h]];
 const choices=targets.map(([x,y])=>groups.filter(g=>g.hits>=2).sort((a,b)=>((a.x-x)/w)**2+((a.y-y)/h)**2-((b.x-x)/w)**2-((b.y-y)/h)**2).slice(0,8));
 let best=null,min=Infinity;
 for(const a of choices[0])for(const b of choices[1])for(const c of choices[2])for(const d of choices[3]){
  const list=[a,b,c,d];if(new Set(list).size!==4)continue;
  const dx=(a.x-b.x+c.x-d.x)/w,dy=(a.y-b.y+c.y-d.y)/h;
  const scales=list.map(p=>p.unit);
  if(Math.max(...scales)>1.8*Math.min(...scales))continue;
  const bias=list.reduce((sum,p,i)=>sum+((p.x-targets[i][0])/w)**2+((p.y-targets[i][1])/h)**2,0);
  const score=15*(dx*dx+dy*dy)+.05*bias;
  if(score<min){min=score;best=list}
 }
 return best?.map(g=>[g.x,g.y])||null;
}