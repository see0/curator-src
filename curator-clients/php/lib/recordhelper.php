<?php
require_once('helperfunctions.php');
require_once('CURATORCONFIG.php');

// Returns true if $string is valid UTF-8 and false otherwise.
function is_utf8($string) {
   
    // From http://w3.org/International/questions/qa-forms-utf-8.html
    return preg_match('%^(?:
          [\x09\x0A\x0D\x20-\x7E]            # ASCII
        | [\xC2-\xDF][\x80-\xBF]             # non-overlong 2-byte
        |  \xE0[\xA0-\xBF][\x80-\xBF]        # excluding overlongs
        | [\xE1-\xEC\xEE\xEF][\x80-\xBF]{2}  # straight 3-byte
        |  \xED[\x80-\x9F][\x80-\xBF]        # excluding surrogates
        |  \xF0[\x90-\xBF][\x80-\xBF]{2}     # planes 1-3
        | [\xF1-\xF3][\x80-\xBF]{3}          # planes 4-15
        |  \xF4[\x80-\x8F][\x80-\xBF]{2}     # plane 16
    )*$%xs', $string);
   
} // function is_utf8}

function get_record($text, $view_names, $force=false, $c_host="", $c_port=0) {
    global $curator_hostname;
    global $curator_port;
    if ($c_host == "") {
        $c_host = $curator_hostname;
    }
    if ($c_port == 0) {
        $c_port = $curator_port;
    }

    $text = stripslashes($text);
    $update = $force;

    $pos = isset($params['pos']) && $params['pos'] == 'true';
    $tokens = isset($params['tokens']) && $params['tokens'] == 'true';
    $chunk = isset($params['chunk']) && $params['chunk'] == 'true';
    $ner = isset($params['ner']) && $params['ner'] == 'true';
    $srl = isset($params['srl']) && $params['srl'] == 'true';
    $nom = isset($params['nom']) && $params['nom'] == 'true';
    $charniak = isset($params['charniak']) && $params['charniak'] == 'true';
    $stanford = isset($params['stanford']) && $params['stanford'] == 'true';
    $coref = isset($params['coref']) && $params['coref'] == 'true';
    $quantities = isset($params['quantities']) && $params['quantities'] == true;

    try {
        $timeout = 90;
        $socket = new TSocket($c_host, $c_port);
        $socket->setRecvTimeout($timeout*1000);
        $transport = new TBufferedTransport($socket, 1024, 1024);
        $transport = new TFramedTransport($transport);
        $protocol = new TBinaryProtocol($transport);
        $client = new CuratorClient($protocol);
        
        $transport->open();        
        foreach ($view_names as $view_name) {
            $record = $client->provide($view_name, $text, $update);
        }
        $transport->close();

        return $record;
    } catch (base_AnnotationFailedException $af) {
        if ($transport->isOpen()) {
            $transport->close();
        }
        echo "<p><code>Annotation failed: $af->reason</p>";
        echo "<code><pre>";
        echo $af;
        echo "</pre></code>";
    } catch (TException $tx) {
        if ($transport->isOpen()) {
            $transport->close();
        }
        echo "<p><code>text sent:</code><pre>".$text."</pre></p>";
        echo "<p><code>Is utf8? " . is_utf8($text)."</code></p>";
        echo "<p><code>Errors:==></code><code><pre>".$tx->getMessage()."</pre></code></p>";
        echo "<p><code><pre>".$tx->reason."</pre></code></p>";
        echo "<p><code><pre>".$tx."</pre></code></p>";
    }
}
?>
