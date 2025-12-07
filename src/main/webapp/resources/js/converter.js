function el2en(text) {
    // Σε άδειο text return;
    if (text == '') return '';
    
    // δίφθογγοι
    expressions = { 
        'αι'                    : 'ai', 
        'αυ([θκξπσςτφχψ]|\\s|$)': 'af$1', 
        'αυ'                    : 'av', 
        'οι'                    : 'oi', 
        'ου'                    : 'ou', 
        'ει'                    : 'ei', 
        'ευ([θκξπσςτφχψ]|\\s|$)': 'ef$1', 
        'ευ'                    : 'ev', 
        '(^|\\s)μπ'             : '$1b', 
        'μπ(\\s|$)'             : 'b$1', 
        'μπ'                    : 'mp', 
        'ντ'                    : 'nt', 
        'τσ'                    : 'ts', 
        'τζ'                    : 'tz', 
        'γγ'                    : 'ng', 
        'γκ'                    : 'gk', 
        'ηυ([θκξπσςτφχψ]|\\s|$)': 'if$1', 
        'ηυ'                    : 'iy', 
        'θ'                     : 'th', 
        'χ'                     : 'ch', 
        'ψ'                     : 'ps' 
    };
    
    text = text.toLowerCase();
    for (var expression in expressions) { 
        text = text.replace(new RegExp(expression, 'g'), expressions[expression]);
    } 

    GR  = 'αάβγδεέζηήθιίϊκλμνξοόπρσςτυύϋφχψωώ';
    ENG = 'aavgdeezii.iiiklmnxooprsstyyyf..oo'; 
    var output = '';
    for (var i = 0; i < text.length; i++) { 
        var ch = text.charAt(i) 
        var pos = GR.indexOf(ch) 
        output += (pos < 0) ? ch : ENG.charAt(pos) 
    } 
    return output.toUpperCase() 
}
