<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
  <head>            
{literal}                                                            

      <style type="text/css">     
    body{background-color:#fff;color:#333;font-family:Arial,Verdana,sans-serif;font-size:62.5%;margin:5% 5% 0 5%;}
    #container{clear:both;font-size:1.65em;margin:auto;line-height:115%;}

    p { font-family: monospace; clear: both; }
    h2 { clear: both; }
    .highlight           { background: #bdf8ff; color: black; }                  
    #top { 
        background-color: white;
        position:fixed;
        overflow: auto;
        top:0px;
        height: 200px;
        width: 90%;
    }
    #rest {
        padding-top: 210px;
    }
    .label {
        color: blue;
    }
    
    .edge {
        color: green;
    }
    
    .attribute {
        color: orange;
    }
    
    .info { font-family: Arial,Verdana,sans-serif; }
      </style>                                                                       
                                                                                     
      <script type="text/javascript" src="./jquery.js"></script>                       
      <script type="text/javascript" src="./highlight.js"></script>                    
      <script type="text/javascript">
        $(document).ready( function() {
{/literal}

        {$jquery}
{literal}
         });                                                                          
      </script>                                                                      
{/literal}

<meta http-equiv="content-type" content="text/html; charset=utf-8">
      <title>Curator Demo</title>                                                            
      </head>                                                                        
  <body>
<div id="container">   
<div id="top"><h1>Curator Demo</h1>
<p id="text">{$text}</p></div>
<div id="rest">
<p class="info">Below are all the annotations available for the input text.  Click words and labels in the annotations below to see where they occur in the input text. Color code: <span class="label">label</span>, <span class="attribute">attribute</span>, <span class="edge">edge</span>, text coverage.</p>

{$content}

</div>
</div>
</body>                                                                              
</html>
