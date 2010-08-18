<?php
ini_set('error_reporting', E_ALL);
ini_set('display_errors',1);
$GLOBALS['THRIFT_ROOT'] = 'thrift';
require_once $GLOBALS['THRIFT_ROOT'].'/Thrift.php';
require_once $GLOBALS['THRIFT_ROOT'].'/protocol/TBinaryProtocol.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TSocket.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/THttpClient.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TBufferedTransport.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TFramedTransport.php';

require_once $GLOBALS['THRIFT_ROOT'].'/packages/curator/Curator.php';

function perform() {
    try {
        $timeout = 300;
        $socket = new TSocket('grandpa.cs.uiuc.edu', 9010);
        $socket->setRecvTimeout($timeout*1000);
        $transport = new TBufferedTransport($socket, 1024, 1024);
        $transport = new TFramedTransport($transport);
        $protocol = new TBinaryProtocol($transport);
        $client = new CuratorClient($protocol);
        
        $transport->open();
        $response = $client->ping();
        $transport->close();
        $content = "<p>The Curator is <span class='status up'>up</span>!";
        $content .= "<br/><a href='./'>demo</a></p>";

        $transport->open();
        $annotators = $client->describeAnnotations();
        $transport->close();
        $content .= "<div id='info'>";
        $content .= "<p>The following annotations are available:</p>";
        $content .= "<dl>";
        foreach ($annotators as $view_name => $annotator) {
            $content .= "<dt><span class='view'>" . $view_name ."</span></dt>";
            $content .= "<dd>".$annotator."</dd>";
        }
        $content .= "</dl>";
        $content .= "</div>";
        return $content;
    } catch (TException $tx) {
        if ($transport->isOpen()) {
            $transport->close();
        }
        $content = "<p>The Curator is <span class='status down'>down</span>!</p>";

        $content .= "<div id='info'><p>Here is the exception:</p>";
        $content .= "<p><code><pre>".$tx->getMessage()."</pre></code></p>";
        $content .= "<code><pre>";
        $content .= $tx;
        $content .= "</pre></code></div>";
        return $content;
    }
}


?>

<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=utf-8"/>
    <title>Curator Status</title>
    <style type="text/css">
    body{background-color:#fff;color:#333;font-family:Arial,Verdana,sans-serif;font-size:62.5%;margin:5% 5% 0 5%;text-align:center;}
    a,a:visited,a:active{color:#0080ff;text-decoration:underline;}
    a:hover{text-decoration:none;}
    input[type=text]{border:1px solid #ccc;color:#ccc;font-size:1em;padding:4px 6px 4px 6px;}
    .status{font-weight:bold;}
    .view{font-weight:bold;}
    .up{color:green;}
    .down{color:red;}
    #info {text-align:left; font-size:0.55em;}
    #container{clear:both;font-size:3em;margin:auto;}
    </style>
  </head>
  <body>
    <div id="container">
                     <?php echo perform(); ?>
    </div>
  </body>
</html>