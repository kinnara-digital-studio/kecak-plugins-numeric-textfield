function decimalTerbilang(num, isCurrency){
    if(num.toString().search(/\./) >= 0)
        var revDecimal = num.toString().replace(/[0-9]+\./, '').split('', 2).reverse();

      var result = '';

      // handle decimal
      if(revDecimal) {
        if(revDecimal.length == 1){
            revDecimal[1] = revDecimal[0];
            revDecimal[0] = '0';
        }

        for(var i = 0; i < 2; i++){
            if(revDecimal[i] != '0') {
              if((i % 3) == 0) {
                if(revDecimal[i + 1] == '1') {
                  result = toWords(revDecimal, i) + 'belas ' + result;
                  i++;
                } else {
                  result = toWords(revDecimal, i)  + result;
                }
              } else if((i % 3) == 1) {
                result = toWords(revDecimal, i) + 'puluh '  + result;
              }
            }else{
                result = toWords(revDecimal, i) + result;
            }
        }

        if(isCurrency)
        result = result + 'sen';
        else
        result = 'koma ' + result;
      }
    return result;

      function toWords(arr, index, decimal) {
        var number = arr[index];
        switch(number) {
          case '0':
            if(index == 1)
              return 'nol ';
            return '';
          case '1':
              if((index == 1 || index == 2 || arr[index + 1]  == '1' || (index % 3) == 1 || (index % 3) == 2))
                return 'se';
              return 'satu ';
          case '2':
            return 'dua ';
          case '3':
            return 'tiga ';
          case '4':
            return 'empat ';
          case '5':
            return 'lima ';
          case '6':
            return 'enam ';
          case '7':
            return 'tujuh ';
          case '8':
            return 'delapan ';
          case '9':
            return 'sembilan ';
        }
      }

}