// Screenshot-only demonstration. This file is never part of the shipped APK.
// It provides a staged camera scene, not a real camera or a decoded message.
if(new URLSearchParams(location.search).get('cameraDemo')==='1'){
  const canvas=document.createElement('canvas');canvas.width=720;canvas.height=1280;
  const context=canvas.getContext('2d'),scene=new Image();scene.src='/camera-demo.jpg';
  const ready=new Promise((resolve,reject)=>{scene.onload=resolve;scene.onerror=reject});
  function draw(){if(scene.complete&&scene.naturalWidth){const scale=Math.max(canvas.width/scene.naturalWidth,canvas.height/scene.naturalHeight);context.drawImage(scene,(canvas.width-scene.naturalWidth*scale)/2,(canvas.height-scene.naturalHeight*scale)/2,scene.naturalWidth*scale,scene.naturalHeight*scale)}requestAnimationFrame(draw)}draw();
  const label={es:'Cámara simulada',ca:'Càmera simulada',en:'Simulated camera'};
  Object.defineProperty(navigator,'mediaDevices',{configurable:true,value:{
    getUserMedia:async constraints=>{if(constraints.audio)throw new DOMException('Demo has no microphone','NotAllowedError');await ready;return canvas.captureStream(24)},
    enumerateDevices:async()=>[{kind:'videoinput',deviceId:'lumen-demo-camera',groupId:'lumen-demo',label:label[document.documentElement.lang]||label.en}],
    getSupportedConstraints:()=>({deviceId:true,width:true,height:true,frameRate:true,facingMode:true})
  }});
  document.addEventListener('DOMContentLoaded',()=>{const stage=document.querySelector('#cameraStage');if(stage){const badge=document.createElement('span');badge.textContent=label[document.documentElement.lang]||label.en;badge.style.cssText='position:absolute;z-index:5;left:12px;top:54px;background:#0b172cdd;color:#fff;border:1px solid #91e5cf;padding:5px 8px;border-radius:8px;font:600 12px/1.4 system-ui;pointer-events:none';stage.append(badge)}});
}
