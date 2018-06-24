module.exports = {
  repeat: function(str, num) {
           if (str.length === 0 || num <= 1) {
               if (num === 1) {
                   return str;
               }

               return '';
           }

           var result = '',
               pattern = str;

           while (num > 0) {
               if (num & 1) {
                   result += pattern;
               }

               num >>= 1;
               pattern += pattern;
           }

           return result;
  },
  lpad: function lpad(obj, str, num) {
           return repeat(str, num - obj.length) + obj;
  },
  uint8toBinaryString: function (uint8) {
           var string = '';
           Array.prototype.forEach.call(uint8, function (element) {
               string += lpad(element.toString(2), "0", 8);
           });
           return string
  },
  rgbToRgba: function (rgbArray) {
           var sourceLength = rgbArray.length;
           var shouldBeLength = sourceLength + sourceLength/3;
           var rgbaArray = new Uint8ClampedArray(shouldBeLength);
           var sourceIndex = 0;
           var destinationIndex = 0;

           while(destinationIndex < shouldBeLength) {
               rgbaArray[destinationIndex++] = rgbArray[sourceIndex++];
               rgbaArray[destinationIndex++] = rgbArray[sourceIndex++];
               rgbaArray[destinationIndex++] = rgbArray[sourceIndex++];
               destinationIndex++
           }
           return rgbaArray
  },
  binaryStringToRgbaUint8Array: function (string) {
           var arr = new Uint8ClampedArray(string.length * 4);
           var i = 0;
           var binaryIndex = 0;
           while(i < string.length * 4) {
               if (string[binaryIndex] === "1") {
                   arr[i++] = 20
                   arr[i++] = 200 //mostly green
                   arr[i++] = 20
                   arr[i++] = 200 //alpha around 0.8
               } else {
                   i++
                   i++
                   i++
                   i++
               }
               binaryIndex++
           }
           return arr
  },
  str2ab32: function (str) {
           var arrayBuffer = new ArrayBuffer(str.length / 8);
           var uintView = new Uint32Array(arrayBuffer);
           for (var i = 0, d = 0 ; d <= str.length ; i++, d = d + 32) {
               uintView[i] = parseInt(str.slice(d, d + 32), 2)
           }
           return uintView;
       }
};
