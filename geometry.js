// Shared mapping for the displayed camera image, touch point and search crop.
export function cameraGeometry(videoWidth,videoHeight,viewWidth,viewHeight){
 const scale=Math.max(viewWidth/videoWidth,viewHeight/videoHeight),offsetX=(viewWidth-videoWidth*scale)/2,offsetY=(viewHeight-videoHeight*scale)/2;
 return {scale,offsetX,offsetY,visible:{x:-offsetX/scale,y:-offsetY/scale,width:viewWidth/scale,height:viewHeight/scale}};
}
export function pointInView(point,videoWidth,videoHeight,geometry){return {x:point.x*videoWidth*geometry.scale+geometry.offsetX,y:point.y*videoHeight*geometry.scale+geometry.offsetY};}
export function pointInVideo(x,y,videoWidth,videoHeight,geometry){return {x:Math.max(0,Math.min(1,(x-geometry.offsetX)/(videoWidth*geometry.scale))),y:Math.max(0,Math.min(1,(y-geometry.offsetY)/(videoHeight*geometry.scale)))};}
